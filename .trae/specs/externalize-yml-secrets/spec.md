# 外置 application.yml 敏感信息 Spec

## Why

[magic-api-server/src/main/resources/application.yml](file:///d:/project/magic-api/magic-api-server/src/main/resources/application.yml) 中硬编码了 4 项敏感凭据（L28-29 编辑器账号 admin/123456、L34-35 TOPIAM client-id/client-secret），且该文件**已被 git 跟踪并推送到 GitHub**（origin: defaultw/magic-api），提交即泄露。需要将凭据外置为「不入库的本地文件 / 环境变量」，并同步更新部署文档。

**已确认事实**：
- 远程仓库已存在 → 历史提交中已含明文密钥。仅改当前文件不能消除历史泄露，**必须轮换凭据**（TOPIAM 侧重置 client-secret、更换编辑器密码），轮换属用户在 TOPIAM 控制台的人工操作，本 spec 只负责代码侧外置 + 文档提示
- Spring Boot 2.4.5 支持 `spring.config.import: optional:file:./application-local.yml`（可选导入，文件不存在不报错）；OS 环境变量优先级高于该导入文件，两层机制可共存
- magic-api-server/.gitignore 现有 `magic-api-files/`、`target/`，需追加 `application-local.yml`

## What Changes

- `application.yml`：删除 4 项明文凭据，改为占位符形式 `${MAGIC_API_SECURITY_USERNAME:}`、`${MAGIC_API_SECURITY_PASSWORD:}`、`${MAGIC_API_OAUTH2_CLIENT_ID:}`、`${MAGIC_API_OAUTH2_CLIENT_SECRET:}`（默认空，未注入时启动不报错但 OIDC/密码登录不可用，强制使用者提供凭据）；文件头部注释补充凭据注入说明
- 新增 `magic-api-server/application-local.yml.example`（**跟踪入库的模板**）：含 4 项凭据键与注释说明，使用者复制为 `application-local.yml` 填真值
- 新增 `magic-api-server/application-local.yml`（**本地真值文件，不入库**）：填入当前 4 项值，保证本机继续可用
- `magic-api-server/.gitignore` 与根 `.gitignore`：追加 `application-local.yml`
- [DEPLOY.md](file:///d:/project/magic-api/magic-api-server/DEPLOY.md)：新增「敏感配置管理」章节——生产用 systemd `EnvironmentFile=/opt/magic-api/magic-api.env`（chmod 600，不入库）注入 4 个环境变量，或在工作目录放 application-local.yml；unit 示例同步加 `EnvironmentFile` 行；补充「凭据已泄露需轮换」警告
- **不做**：git 历史清洗（filter-repo）——超出最小改动，以轮换凭据替代；不改 issuer/redirect-uri/scopes（非凭据，用户未标记）

## Impact

- Affected specs: 无
- Affected code: `magic-api-server/src/main/resources/application.yml`、新增 `application-local.yml.example` / `application-local.yml`、`magic-api-server/.gitignore`、根 `.gitignore`、`magic-api-server/DEPLOY.md`
- 风险：忘记创建 application-local.yml 的环境下 OIDC 登录失效（属预期，注释与 example 文件引导）

## ADDED Requirements

### Requirement: 配置文件零明文凭据

application.yml SHALL 不包含任何明文密码/client-secret，凭据一律通过环境变量占位符 `${...}` 引用；真值仅存在于 gitignore 排除的 `application-local.yml` 或运行时环境变量中。

#### Scenario: 仓库内无敏感信息

- **WHEN** `git grep -iE "123456|dfd09312|f50b5959"`（在跟踪文件范围内）且查看 application.yml
- **THEN** 无任何明文密码/secret 命中；application-local.yml 不出现在 `git status` 待提交列表

#### Scenario: 本机开发不受影响

- **WHEN** 重新打包并以存在 application-local.yml 的工作目录启动 jar
- **THEN** 启动无报错，`/magic/web` 200，OIDC 登录配置完整加载（config 中 client-id 为真值）

### Requirement: 部署文档含凭据注入与轮换指引

DEPLOY.md SHALL 提供 systemd EnvironmentFile 注入方式（含权限 600 要求）、application-local.yml 方式，及「历史已泄露、须轮换 client-secret 与编辑器密码」的警告。

#### Scenario: 文档可指导安全部署

- **WHEN** 按 DEPLOY.md 新章节在 Ubuntu 配置 magic-api.env 并启动
- **THEN** 服务正常加载凭据，magic-api.env 与 application-local.yml 均不在 git 跟踪范围内
