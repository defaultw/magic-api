# Tasks
- [x] Task 1: 更新 magic-api-demo application.yml
  - [x] SubTask 1.1: `use-pkce: true`（用户已手动改值，本次修正注释为「TOPIAM 强制要求 PKCE，必须开启（关闭会报 invalid_request: code_challenge）」）
  - [x] SubTask 1.2: 修正 password-login-enabled 过时注释（false：禁用密码登录，仅 OIDC；true：启用 admin/123456 密码回退）
- [x] Task 2: 重新打包启动并验证
  - [x] SubTask 2.1: `mvn -pl magic-api-demo -am clean package -DskipTests` 打包成功（exit 0）
  - [x] SubTask 2.2: 后台启动 jar（cwd=magic-api-demo），302 Location 验证通过：redirect_uri=http://127.0.0.1:9999/magic/web/oauth2/callback、scope=openid、code_challenge=G2OvI3_...、code_challenge_method=S256

# Task Dependencies
- Task 2 depends on Task 1
