# 默认账号密码登录并暴露认证配置 Spec

## Why

当前 `application.yml` 默认 `oauth2.enabled=true` 且 `password-login-enabled=false`：Docker 镜像部署后若未配置 TOPIAM OIDC 凭据，登录框只有「统一认证登录」按钮、账号密码表单被隐藏，**开箱必然无法登录**。且 `oauth2.enabled` / `password-login-enabled` / `auto-redirect` 三个键没有环境变量占位符，只能挂载配置文件覆盖，容器化运维不便。

## What Changes

- **BREAKING**（行为翻转）：`application.yml` 认证默认值调整
  - `magic-api.security.oauth2.enabled` → `${MAGIC_API_OAUTH2_ENABLED:false}`（统一认证默认**关闭**）
  - `magic-api.security.oauth2.password-login-enabled` → `${MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED:true}`（密码登录默认**开启**）
  - `magic-api.security.username` → `${MAGIC_API_SECURITY_USERNAME:admin}`、`password` → `${MAGIC_API_SECURITY_PASSWORD:123456}`（内置公开默认凭据，非个人秘密；文档强提示生产必须覆盖）
  - `magic-api.security.oauth2.auto-redirect` → `${MAGIC_API_OAUTH2_AUTO_REDIRECT:false}`（可选暴露）
  - 文件头注释同步改写（默认凭据与生产覆盖警告、OIDC 改为可选说明）
- `magic-api.env.example`：新增上述 3 个变量条目，修正「密码登录默认关闭」的过时注释
- `magic-api-server/DOCKERHUB_README.md`（Docker Hub 页面）：环境变量表补全 + 新增「登录认证」章节（默认账号 admin/123456、改密方法、OIDC 启用条件与所需全部变量、MCP token 说明、安全警告）
- `magic-api-server/DEPLOY-DOCKER.md`：第七节凭据表补全 3 变量；第八节「启用密码登录需挂载 config/application.yml」改为环境变量方式（挂载方式保留为 redirect-uri 等高级覆盖）；第九节 OIDC 标注为可选启用；第三节/FAQ 同步默认凭据说明
- 重建镜像并逐场景验证

## Impact

- Affected specs: `externalize-yml-secrets`（MODIFIED：「凭据禁止明文入库」原则保留——admin/123456 属公开默认值而非个人凭据，文件头需明确区分）; `dockerize-magic-api-server`（凭据注入 Requirement 场景更新）
- Affected code:
  - 修改：`magic-api-server/src/main/resources/application.yml`、`magic-api-server/magic-api.env.example`、`magic-api-server/DOCKERHUB_README.md`、`magic-api-server/DEPLOY-DOCKER.md`
  - 不改动 Java 代码、Dockerfile、workflow
- 技术事实（已核实）：`Security.Oauth2.passwordLoginEnabled` Java 默认 true；`DefaultAuthorizationInterceptor` 仅做用户名密码比对，oauth2.enabled=false 时不装配 OAuth2 拦截器，密码登录链路完整可用

## ADDED Requirements

### Requirement: 默认开箱即用账号密码登录

Docker 镜像 SHALL 在未注入任何环境变量时默认提供账号密码登录（admin/123456），且不出现统一认证（OIDC）入口。

#### Scenario: 零配置启动可登录

- **WHEN** 不注入任何凭据环境变量启动容器
- **THEN** `config.json` 返回 `oauth2.enabled=false`，登录框仅显示账号密码表单（无 SSO 按钮）
- **AND** 使用 admin/123456 登录成功进入工作台

#### Scenario: 环境变量覆盖默认凭据

- **WHEN** 注入 `MAGIC_API_SECURITY_USERNAME` / `MAGIC_API_SECURITY_PASSWORD` 后重启
- **THEN** 自定义凭据登录成功，admin/123456 失效

### Requirement: 认证开关全部环境变量可配

`oauth2.enabled`、`oauth2.password-login-enabled`、`oauth2.auto-redirect` SHALL 均可经环境变量覆盖，无需挂载配置文件。

#### Scenario: 环境变量启用 OIDC

- **WHEN** 注入 `MAGIC_API_OAUTH2_ENABLED=true` + `MAGIC_API_OAUTH2_CLIENT_ID/SECRET`（`issuer`/`redirect-uri` 用内置默认）后重启
- **THEN** 登录框出现「统一认证登录」按钮，OIDC 授权码流程可用
- **AND** 密码登录仍可用（password-login-enabled 默认 true）

#### Scenario: 恢复仅 OIDC 模式

- **WHEN** 注入 `MAGIC_API_OAUTH2_ENABLED=true` + `MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED=false`
- **THEN** 密码表单隐藏，仅 SSO 按钮可用（兼容旧行为）

#### Scenario: MCP token 鉴权

- **WHEN** 注入 `MAGIC_API_MCP_TOKEN` 后请求 `/mcp/magic`（无 token 头）
- **THEN** 返回 401

### Requirement: Docker Hub 页面认证文档完整

`DOCKERHUB_README.md` SHALL 包含完整的登录认证章节：默认凭据、修改方式、OIDC 启用的全部变量与前置条件（issuer/回调白名单）、安全警告，环境变量表与实际占位符一一对应。

#### Scenario: 按 README 可完成认证配置

- **WHEN** 用户仅按 Docker Hub 页面文档操作
- **THEN** 可完成改密 / 启用 OIDC / 配置 MCP token，无需查阅其他文档

## REMOVED Requirements

### Requirement: 密码登录需挂载配置文件启用（DEPLOY-DOCKER.md 第八节原描述）

**Reason**: 密码登录默认开启，挂载 `/app/config/application.yml` 启用密码登录的操作不再必要（挂载方式保留用于 redirect-uri 等键的高级覆盖）。
**Migration**: 文档第八节改写为环境变量方式；原挂载示例降级为「高级：覆盖任意配置键」。
