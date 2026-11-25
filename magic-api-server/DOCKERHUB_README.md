# magic-api-server

[magic-api](https://github.com/ssssssss-team/magic-api) 是一个基于 Spring Boot 的 HTTP API 快速开发框架，内置 Web 编辑器，可在浏览器中用脚本直接开发接口并实时发布，无需重启服务。本镜像为服务端开箱即用版本（Java 8 运行时，内置全部官方插件）。

## 快速开始

```bash
docker pull wangxiaojiang10088/magic-api-server:latest

# 生产环境请务必修改默认密码 admin/123456
docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v /data/magic-api:/data/magic-api \
  -e TZ=Asia/Shanghai \
  -e MAGIC_API_SECURITY_PASSWORD=changeit \
  --restart unless-stopped \
  wangxiaojiang10088/magic-api-server:latest
```

启动后访问编辑器：`http://localhost:9999/magic/web/index.html`

接口列表：`http://localhost:9999/magic/web/api.html`

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `MAGIC_API_SECURITY_USERNAME` | 编辑器登录用户名 | `admin` |
| `MAGIC_API_SECURITY_PASSWORD` | 编辑器登录密码 | `123456` |
| `MAGIC_API_OAUTH2_ENABLED` | 统一认证（OIDC/SSO）开关 | `false` |
| `MAGIC_API_OAUTH2_CLIENT_ID` | OIDC 客户端 ID（启用 SSO 时必填） | 空 |
| `MAGIC_API_OAUTH2_CLIENT_SECRET` | OIDC 客户端密钥（启用 SSO 时必填） | 空 |
| `MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED` | OIDC 启用时是否保留密码登录 | `true` |
| `MAGIC_API_OAUTH2_AUTO_REDIRECT` | 打开编辑器自动跳转 OIDC 授权页 | `false` |
| `MAGIC_API_MCP_TOKEN` | MCP 服务访问令牌（配置后 /mcp/magic 需携带 `X-Mcp-Token`） | 空 |
| `MAGIC_API_RESOURCE_LOCATION` | 数据持久化目录 | `/data/magic-api` |
| `TZ` | 时区 | `Asia/Shanghai` |
| `JAVA_OPTS` | JVM 参数 | `-Xms256m -Xmx512m` |

## 登录认证

### 默认：账号密码登录（开箱即用）

- 用户名 `admin`，密码 `123456`（**公开默认凭据**，仅用于首次体验）
- 生产环境务必修改：

```bash
docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v /data/magic-api:/data/magic-api \
  -e MAGIC_API_SECURITY_USERNAME=myuser \
  -e MAGIC_API_SECURITY_PASSWORD=<你的强密码> \
  --restart unless-stopped \
  wangxiaojiang10088/magic-api-server:latest
```

### 可选：统一认证（OIDC/SSO）

镜像默认**关闭** OIDC。接入企业身份提供方（TOPIAM、Keycloak 等）时额外注入：

```bash
-e MAGIC_API_OAUTH2_ENABLED=true \
-e MAGIC_API_OAUTH2_CLIENT_ID=<client-id> \
-e MAGIC_API_OAUTH2_CLIENT_SECRET=<client-secret>
```

前置条件与说明：

1. 回调地址默认 `http://127.0.0.1:9999/magic/web/oauth2/callback`，必须加入 IdP 应用「登录 Redirect URI 白名单」；改用 IP/域名/端口访问时，同步覆盖环境变量 `MAGIC_API_SECURITY_OAUTH2_REDIRECTURI` 并在 IdP 添加新地址。
2. issuer 默认指向内置 TOPIAM 实例，接入其他 IdP 时覆盖 `MAGIC_API_SECURITY_OAUTH2_ISSUER=https://<idp-host>/...`。
3. 企业管控场景可设 `MAGIC_API_SECURITY_PASSWORD_LOGIN_ENABLED=false` 隐藏密码表单，仅保留 SSO 登录。
4. 完整说明（含白名单逐字符一致要求）见 [DEPLOY-DOCKER.md 第七、九节](https://github.com/defaultw/magic-api/blob/main/magic-api-server/DEPLOY-DOCKER.md)。

> 安全提示：`admin/123456` 为公开默认凭据，公网部署务必修改；MCP 端点建议配置 `MAGIC_API_MCP_TOKEN` 鉴权。

## 数据持久化

所有接口定义、函数、数据源、任务配置保存在 `/data/magic-api`。务必挂载到宿主机或数据卷，否则容器删除后数据丢失：

```bash
-v /data/magic-api:/data/magic-api
```

> 注意：数据目录挂载属主需允许 uid 10001（容器以非 root 用户运行）。

## 标签说明

| 标签 | 说明 |
|---|---|
| `latest` | 最新发布版本 |
| `<版本>`（如 `2.2.2`） | 与同名 GitHub Release 对应的固定版本 |

## 健康检查

镜像内置 HEALTHCHECK，探测 `http://127.0.0.1:9999/magic/web/index.html`，启动宽限期 60 秒。`docker ps` 中 STATUS 显示 `(healthy)` 即正常。

## 完整文档

部署指南（构建、数据迁移、凭据配置、OIDC 对接、备份恢复、升级与 FAQ）：

https://github.com/defaultw/magic-api/blob/main/magic-api-server/DEPLOY-DOCKER.md

源码仓库：https://github.com/defaultw/magic-api

上游项目：https://github.com/ssssssss-team/magic-api
