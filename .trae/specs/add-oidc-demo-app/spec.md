# OIDC 功能验证子工程 Spec

## Why
OIDC 统一认证已在 `add-oidc-unified-auth` 中实现并编译通过，但缺少一个可运行的最小工程实际验证完整 SSO 流程（跳转 IdP → 授权码换 token → 回调签发内部 Magic-Token → 编辑器免密进入工作台）。需要在当前工程中新建一个 Spring Boot 子工程承载真实 OIDC 配置（对接 TOPIAM），作为端到端验证载体，后续也可作为接入模板。

## What Changes
- 在父工程 pom 的 `<modules>` 中新增 `<module>magic-api-demo</module>`
- 新建子工程 `magic-api-demo`（Spring Boot 2.4.5 fat jar，Java 8）：
  - 依赖 `magic-api-spring-boot-starter`（2.2.2，本仓库 reactor 构建）、`spring-boot-starter-web`、`spring-boot-starter-aop`（magic-api 需要）、`h2`（文件模式，接口持久化与测试接口数据源）
  - 接口资源存储 `magic-api.resource.type=file`，location 指向工程内相对目录 `./magic-api-files`（默认 `/data/magic-api/` 在 Windows 不可写/不直观）
  - 置空 `magic-api.prefix`（留空则仅 UI 可访问，业务接口前缀不启用，验证环境最简）
  - 安全配置：`magic-api.security.username/password` 保留密码回退，`magic-api.security.oauth2.*` 对接 TOPIAM（issuer/client-id/client-secret 真实值，client-secret 从用户消息脱敏值 `********` 需用户自填占位 `<你的客户端密钥>`）
  - `auto-redirect: false`（默认走登录框按钮，便于观察完整流程；密码回退保留）
- 新建 Demo 主类 `@SpringBootApplication` + `@EnableMagicAPI`？——**注意**：starter 自动装配无需 `@EnableMagicAPI`（2.x 已移除），仅需 `@SpringBootApplication`
- 新建一个测试脚本 API 文件（保存于 `magic-api-files` 初始目录，供登录后在编辑器内直接运行验证）
- **不修改**既有 magic-api / starter / editor 任何代码（纯增量子工程）

## Impact
- Affected specs: add-oidc-unified-auth（功能验证载体，不改其行为）
- Affected code:
  - `pom.xml`（根：新增 module）
  - 新目录 `magic-api-demo/`：pom.xml、`src/main/java/.../DemoApplication.java`、`src/main/resources/application.yml`、`magic-api-files/`（初始测试接口）

## ADDED Requirements

### Requirement: 可运行验证工程
系统 SHALL 提供可独立启动的 Spring Boot 子工程 magic-api-demo，承载 magic-api + OIDC 完整运行时。

#### Scenario: 启动成功
- **WHEN** 执行 `mvn -pl magic-api-demo -am package -DskipTests` 后运行 `java -jar magic-api-demo/target/magic-api-demo-2.2.2.jar`
- **THEN** 应用在 9999 端口启动成功，访问 `http://localhost:9999/magic/web/index.html` 出现编辑器登录页

#### Scenario: OIDC 按钮/流程可用
- **WHEN** 打开登录页
- **THEN** 登录框出现「统一认证登录 / OIDC Login」按钮（config.json 下发 oauth2.enabled=true），点击后 302 至 TOPIAM 授权端点
- **WHEN** 在 TOPIAM 完成登录回调
- **THEN** 编辑器自动进入工作台（Magic-Token 签发并落库）

#### Scenario: 密码回退可用
- **WHEN** 使用 admin/123456 密码登录
- **THEN** 登录成功进入工作台（password-login-enabled 默认 true）

### Requirement: 配置最小化与真实对接
- issuer/client-id 使用用户提供的 TOPIAM 真实值；client-secret 以占位符形式写入 yml 并注释提醒替换
- redirect_uri 推导 = `http://localhost:9999/magic/web/oauth2/callback`，用户需将该地址加入 TOPIAM 应用「登录 Redirect URI 白名单」（在工程 README 或 yml 注释中说明——不创建独立文档，注释承载）

## REMOVED Requirements
（无）
