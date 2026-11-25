# Tasks

- [x] Task 1: 修改 `application.yml` 默认配置（认证默认值翻转 + 占位符补全）
  - [x] 1.1 `oauth2.enabled` 改为 `${MAGIC_API_OAUTH2_ENABLED:false}`；`password-login-enabled` 改为 `${MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED:true}`；`auto-redirect` 改为 `${MAGIC_API_OAUTH2_AUTO_REDIRECT:false}`
  - [x] 1.2 `username` 占位符默认值改 `admin`、`password` 改 `123456`（公开默认凭据，注释标注生产必须覆盖）
  - [x] 1.3 重写文件头注释：默认密码登录开箱即用；OIDC 变为可选（变量启用）；保留凭据轮换警告
- [x] Task 2: 更新 `magic-api.env.example`（新增 3 个变量条目，修正过时注释）
- [x] Task 3: 更新 `DEPLOY-DOCKER.md`（第七节变量表补全；第八节改环境变量方式，挂载降级为高级覆盖；第九节 OIDC 可选化；第三节/FAQ 默认凭据同步）
- [x] Task 4: 更新 `DOCKERHUB_README.md`（环境变量表补全 + 新增「登录认证」章节：默认凭据/改密/OIDC 启用/MCP token/安全警告）
- [x] Task 5: 重建镜像并验证
  - [x] 5.1 `docker compose build`（或 docker build）成功
  - [x] 5.2 零配置启动：config.json `oauth2.enabled=false`，admin/123456 登录成功
  - [x] 5.3 环境变量覆盖凭据生效
  - [x] 5.4 `MAGIC_API_OAUTH2_ENABLED=true` 时 SSO 按钮出现、password-login-enabled=false 时密码表单隐藏
  - [x] 5.5 MCP token 401 鉴权（如适用）

# Task Dependencies

- Task 5 依赖 Task 1-4
- Task 2/3/4 相互独立、可并行，均依赖 Task 1 确定的最终变量名
