# Checklist

- [x] application.yml：`oauth2.enabled`/`password-login-enabled`/`auto-redirect` 均有环境变量占位符，默认值 false/true/false
- [x] application.yml：username/password 占位符默认值 admin/123456，注释含生产覆盖警告
- [x] application.yml：文件头注释与实际默认行为一致（OIDC 可选、密码登录默认开启）
- [x] magic-api.env.example：含 MAGIC_API_OAUTH2_ENABLED、MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED、MAGIC_API_OAUTH2_AUTO_REDIRECT 三个新条目，注释无过时表述
- [x] DEPLOY-DOCKER.md：变量表含 3 个新变量；第八节为环境变量方式（挂载保留为高级覆盖）；第九节 OIDC 标注可选；无「默认仅 OIDC 登录」等过时表述
- [x] DOCKERHUB_README.md：环境变量表与 application.yml 占位符一一对应；含「登录认证」章节（默认凭据、改密、OIDC 启用全变量、MCP token、安全警告）
- [x] 零配置启动镜像：config.json 返回 oauth2.enabled=false，admin/123456 登录成功，登录框无 SSO 按钮
- [x] 注入自定义凭据后 admin/123456 失效、新凭据可登录
- [x] MAGIC_API_OAUTH2_ENABLED=true 时登录框出现 SSO 按钮
- [x] 挂载的 application-local.yml / magic-api.env 路径行为不受影响（optional import 兼容）
