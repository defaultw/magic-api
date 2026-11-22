# Checklist

- [x] application.yml 内无明文密码/secret，均为 `${ENV:}` 占位符
- [x] application-local.yml.example 存在且入库；application-local.yml 存在且被 gitignore 排除（check-ignore 通过）
- [x] 根与 server .gitignore 均含 application-local.yml
- [x] DEPLOY.md 含「敏感配置管理」章节（EnvironmentFile 600 权限 + local.yml 方式 + 轮换警告），unit 示例含 EnvironmentFile
- [x] git grep 无 123456/dfd09312/f50b5959 命中（跟踪文件范围）
- [x] 重新打包后启动 /magic/web 200；注入真值（env 或 local.yml）后 OIDC 配置完整加载
  - 空占位符启动：/magic/web 200（启动不报错）
  - 真值加载：工作目录放 application-local.yml 后，`/magic/web/oauth2/authorize` 返回 302，服务日志输出「跳转OAuth2授权端点: https://iam.xiaojiang.wang/...client_id=f50b5959...&code_challenge=...&code_challenge_method=S256」——issuer OIDC 发现、client-id/secret、PKCE 均已完整加载
