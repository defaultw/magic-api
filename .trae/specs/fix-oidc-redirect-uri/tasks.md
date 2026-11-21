# Tasks
- [x] Task 1: 更新 magic-api-demo application.yml 的 OIDC 配置
  - [x] SubTask 1.1: 显式固定 `redirect-uri`（依据运行时证据：TOPIAM 白名单已确认为 127.0.0.1 版本，故取 `http://127.0.0.1:9999/magic/web/oauth2/callback`）
  - [x] SubTask 1.2: 显式 `use-pkce: false`（截图 TOPIAM PKCE 开关为关，须对齐）；追加 `scopes: openid`（上次实测 TOPIAM 返回 invalid_scope 拒绝了 profile，先用最小集）
- [x] Task 2: 重新打包启动并验证
  - [x] SubTask 2.1: `mvn -pl magic-api-demo -am clean package -DskipTests` 打包成功（exit 0）
  - [x] SubTask 2.2: 启动 jar，验证 `/magic/web/oauth2/authorize` 302 Location：redirect_uri 解码后为 `http://127.0.0.1:9999/magic/web/oauth2/callback`，scope=openid，无 PKCE 参数

# Task Dependencies
- Task 2 depends on Task 1
- （用户侧前置，非本任务）TOPIAM 白名单需包含 `http://127.0.0.1:9999/magic/web/oauth2/callback`，且 PKCE 开关与 use-pkce=false 一致；若需用户名 claim（preferred_username），需在 TOPIAM 应用侧启用 profile 并把 scopes 改回 "openid profile"

# 附加修复（与原始计划偏差说明）
- 原计划 redirect-uri 取 localhost、use-pkce 取 true；执行期间上次运行日志显示 TOPIAM 已接受 127.0.0.1 白名单（回调能携带 error 跳回）且曾返回 invalid_scope（拒绝 profile），PKCE 开关截图为关。按证据调整为 127.0.0.1 / openid / false。
