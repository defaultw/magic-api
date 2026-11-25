# 接口登录鉴权拦截器 Spec（add-api-auth-interceptor）

## Why

业务接口目前可匿名访问。根据官方鉴权文档（<https://www.ssssssss.org/magic-api/pages/security/api/#鉴权流程），需实现接口级登录认证：接口在页面配置「该接口需要登录才允许访问」（require_login）选项后，校验请求必须携带有效> magic-token；账号密码登录与 OIDC 登录均签发 magic-token，需同时兼容。

## What Changes

* magic-api-server 工程新增 `RequireLoginRequestInterceptor`（@Component 实现 `org.ssssssss.magicapi.core.interceptor.RequestInterceptor`），preHandle 逻辑：
  1. `!"true".equals(info.getOptionValue(Options.REQUIRE_LOGIN))` → 返回 null 放行（未开启登录认证的接口行为不变，选项支持接口级与分组继承）
  2. 开启后：取 `Magic-Token` 请求头（`Constants.MAGIC_TOKEN_HEADER`），调用 `configuration.getAuthorizationInterceptor().getUserByToken(token)` 校验
  3. 校验成功 → 返回 null 放行；抛 `MagicLoginException`（token 缺失/无效/过期）→ 返回 `new JsonBean<>(401, "用户未登录")` 中断请求
* 构造器注入 `MagicConfiguration`（该 Bean 由 starter 提供），无需手动注册拦截器——starter 已自动收集所有 RequestInterceptor Bean（MagicAPIAutoConfiguration L359-362）
* 不修改 magic-api 核心模块与 starter 模块，仅 server 工程新增一个类

## Impact

* Affected specs: 无已有规格冲突；与 fix-mcp-token-auth（MCP 端点鉴权）互不影响（MCP 走 Spring MVC HandlerInterceptor，业务接口走 RequestHandler）
* Affected code:
  * 新增 `d:\project\magic-api\magic-api-server\src\main\java\org\ssssssss\magicapi\demo\RequireLoginRequestInterceptor.java`
  * 复用：`MagicConfiguration`、`RequestInterceptor`、`Options`、`Constants`、`JsonBean`、`MagicLoginException`、`MagicUser`

## ADDED Requirements

### Requirement: 接口登录鉴权（require\_login 选项校验）

The system SHALL 在业务接口（走 RequestHandler 的 magic-api 接口）配置了 `require_login=true` 选项时，校验请求头 `Magic-Token` 是否为有效登录会话；未通过则返回 401 并中断接口执行。

#### Scenario: 未开启登录认证的接口不受影响

* **WHEN** 接口及其所属分组均未配置 require\_login 选项
* **THEN** 请求正常执行，返回接口自身结果（与现状一致）

#### Scenario: 开启登录认证且未携带 token

* **WHEN** 接口开启 require\_login=true，请求未携带 Magic-Token 头
* **THEN** 返回 `{"code":401,"message":"用户未登录"}`，接口脚本不执行

#### Scenario: 开启登录认证且 token 无效或过期

* **WHEN** 接口开启 require\_login=true，请求携带的 Magic-Token 无法通过 getUserByToken 校验（不存在或已过期）
* **THEN** 返回 `{"code":401,"message":"用户未登录"}`，接口脚本不执行

#### Scenario: 账号密码登录后访问受保护接口

* **WHEN** 通过 `POST /magic/web/login`（username/password）登录获得响应头 Magic-Token，携带该 token 访问开启 require\_login 的接口
* **THEN** 校验通过，接口正常执行返回结果

#### Scenario: OIDC 登录后访问受保护接口

* **WHEN** 通过 OIDC 统一认证登录（OAuth2AuthorizationInterceptor.createSession 签发内部 token），携带该 token 访问开启 require\_login 的接口
* **THEN** 校验通过，接口正常执行返回结果（同一 getUserByToken 会话校验入口，天然兼容）

#### Scenario: 分组级选项继承生效

* **WHEN** 分组配置 require\_login=true（接口自身未配置），访问该分组下接口未携带有效 token
* **THEN** 返回 401（ApiInfo.getOptionValue 支持分组选项回退）

## 验证方案

1. `mvn -pl magic-api-server -am package` 重新打包，启动 jar
2. 通过工作台/MCP 创建测试接口并勾选 require\_login 选项
3. curl 验证：无 token → 401；伪造 token → 401；账号密码登录取 Magic-Token → 200；不开启选项的接口匿名 → 正常
4. OIDC 链路验证：浏览器完成 OIDC 登录取 token 后访问（或确认与密码登录共用同一会话存储 createSession/sessions，代码路径一致）
