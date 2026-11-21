# OIDC 统一认证接入 Spec

## Why
当前 magic-api 仅支持在 `magic-api.security.username/password` 配置单一固定账号密码登录（`DefaultAuthorizationInterceptor`，MD5 token，永不过期），无法对接企业统一身份认证。需要新增标准 OIDC（OpenID Connect）授权码流程登录，使编辑器 UI 可通过企业 IdP（Keycloak、Authing、Azure AD、钉钉等）SSO 登录，同时保留原有账号密码登录能力作为回退。

## 现状分析（改造依据）
- 认证接口：`AuthorizationInterceptor`（requireLogin / getUserByToken / login / logout / refreshToken / allowVisit）
- 唯一内置实现 `DefaultAuthorizationInterceptor`，由 starter 中 `@ConditionalOnMissingBean` 装配，可被用户自定义 Bean 覆盖
- 配置模型：`Security` 仅含 username/password；控制器为 `MagicWorkbenchController`（`POST /login`、`POST /user`、`POST /logout`、`GET /config.json`），注册于 `{magic-api.web}` 前缀下
- 前端 magic-editor 为预编译 Vue 产物（本仓库无前端源码），但 `magic-editor/src/main/resources/magic-editor/index.html` 是直接服务的可编辑文件
- 前端登录链路：token 存于 localStorage 键 `magic-token`（header 名 `magic-token`），页面加载时自动 `POST /login`（空用户名密码）→ 后端走 `getUserByToken(Magic-Token)` 校验；失败则弹登录框提交用户名密码换 token
- WebSocket 工作台登录（`MagicWorkbenchHandler.onLogin`）同样经 `getUserByToken` 校验

## What Changes
- `Security` 新增 `oauth2` 嵌套配置类：enabled、issuer、client-id、client-secret、scopes（默认 openid,profile）、authorization-uri / token-uri / userinfo-uri（可选覆盖）、redirect-uri（可选）、username-claim（默认 preferred_username）、token-validity-seconds（默认 28800 = 8h）、auto-redirect（默认 false）、password-login-enabled（默认 true）、use-pkce（默认 true）
- 新增 `OAuth2AuthorizationInterceptor implements AuthorizationInterceptor`：内存 token 存储（过期惰性清理）+ 委托原账号密码校验实现回退；不再直接改动 `DefaultAuthorizationInterceptor`
- 新增 `MagicOAuth2Controller`：
  - `GET {web}/oauth2/authorize`：生成一次性 state（5 分钟有效）与 PKCE verifier/challenge，302 跳转 IdP 授权端点
  - `GET {web}/oauth2/callback`：校验 state → 用 code + client 凭证换取 token → 调用 userinfo 获取用户声明 → 生成内部 MagicUser 与随机 Magic-Token 存入 token 存储 → 302 至 `{web}/index.html#magic-token=<token>`
- `MagicAPIAutoConfiguration`：`oauth2.enabled=true` 时装配新拦截器（内部包裹默认密码实现）并注册 `MagicOAuth2Controller`；保持 `@ConditionalOnMissingBean` 用户自定义语义
- `MagicWorkbenchController.readConfig()`：config.json 下发 `oauth2: {enabled, autoRedirect}` 供前端脚本使用
- `magic-editor/index.html` 注入轻量内联脚本（无依赖）：
  - 解析 URL 片段 `#magic-token=` → 写入 localStorage `magic-token` → 清除片段
  - `autoRedirect=true` 且无有效 token 时自动跳转 `{web}/oauth2/authorize`
  - 否则在未登录时向登录框注入「统一认证登录」按钮链接至 authorize 端点
- 不引入任何新第三方依赖（复用 spring-web RestTemplate + 已有 Jackson `JsonUtils`），Java 8 兼容
- 无 **BREAKING** 变更：未开启 oauth2 时所有行为与现状完全一致

## Impact
- Affected specs: 无既有 spec（全新能力）
- Affected code:
  - `magic-api`: `core/config/Security.java`（扩展）、新增 `core/interceptor/OAuth2AuthorizationInterceptor.java`、`core/web/MagicOAuth2Controller.java`
  - `magic-api-spring-boot-starter`: `MagicAPIAutoConfiguration.java`（装配与控制器注册）
  - `magic-editor`: `src/main/resources/magic-editor/index.html`（注入脚本）

## ADDED Requirements

### Requirement: OIDC 授权码登录
系统 SHALL 在 `magic-api.security.oauth2.enabled=true` 时提供标准 OIDC 授权码流程登录入口。

#### Scenario: 未登录用户跳转 IdP
- **WHEN** 用户点击统一认证登录入口（或 auto-redirect 自动跳转）
- **THEN** 后端生成一次性 state 与 PKCE 参数并 302 到 IdP 授权端点（redirect_uri 默认从当前请求推导）

#### Scenario: 回调换取会话
- **WHEN** IdP 携带 code 回调 `{web}/oauth2/callback` 且 state 校验通过
- **THEN** 后端用 code 换取 access_token，调用 userinfo 获取用户标识，签发内部 MagicUser（id=sub，username=username-claim 对应值）并存入 token 存储，302 到 `index.html#magic-token=<token>`

#### Scenario: 编辑器自动登录
- **WHEN** 编辑器页面加载后从 localStorage 读到有效 token
- **THEN** 现有自动登录流程（POST /login 携带 Magic-Token 头）验证通过直接进入工作台，无需再输账号密码

#### Scenario: state 防伪造
- **WHEN** 回调携带的 state 不存在 / 已过期 / 已被使用
- **THEN** 返回 401 且不创建任何会话

### Requirement: token 会话管理
#### Scenario: token 有效期
- **WHEN** 签发的内部 token 超过 `token-validity-seconds`（默认 8 小时）
- **THEN** `getUserByToken` 抛出 `MagicLoginException`，过期条目被惰性清理

#### Scenario: 登出
- **WHEN** 调用现有 `POST /logout`
- **THEN** 服务端删除对应 token，后续校验失败

### Requirement: 账号密码回退
- **WHEN** `password-login-enabled=true`（默认）：原有 `POST /login` 用户名密码流程保持可用
- **WHEN** `password-login-enabled=false`：密码登录抛出 `MagicLoginException`，仅允许 OIDC 登录

### Requirement: OIDC 发现与端点配置
- **WHEN** 配置了 `issuer`：自动请求 `{issuer}/.well-known/openid-configuration` 解析 authorization/token/userinfo 端点（结果缓存）
- **WHEN** 显式配置了 authorization-uri / token-uri / userinfo-uri：优先生效
- redirect-uri 未配置时从当前请求动态推导（contextPath + web + /oauth2/callback）

### Requirement: 兼容性
- oauth2 未开启：所有现有行为不变，包括用户自定义 `AuthorizationInterceptor` Bean 的场景
- WebSocket 工作台登录、权限校验（allowVisit）均经由新拦截器正常工作
- `config.json` 不再下发任何客户端密钥（延续现有 username/password 置空策略）

## REMOVED Requirements
（无）
