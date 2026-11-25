# magic-api-server

[magic-api](https://github.com/ssssssss-team/magic-api) 是一个基于 Spring Boot 的 HTTP API 快速开发框架，内置 Web 编辑器，可在浏览器中用脚本直接开发接口并实时发布，无需重启服务。本镜像为服务端开箱即用版本（Java 8 运行时，内置全部官方插件）。

## 快速开始

```bash
docker pull wangxiaojiang10088/magic-api-server:latest

docker run -d --name magic-api-server \
  -p 9999:9999 \
  -v /data/magic-api:/data/magic-api \
  -e TZ=Asia/Shanghai \
  --restart unless-stopped \
  wangxiaojiang10088/magic-api-server:latest
```

启动后访问编辑器：`http://localhost:9999/magic/web/index.html`

接口列表：`http://localhost:9999/magic/web/api.html`

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `MAGIC_API_SECURITY_USERNAME` | 编辑器登录用户名（设置后开启密码登录） | 未启用 |
| `MAGIC_API_SECURITY_PASSWORD` | 编辑器登录密码 | 未启用 |
| `MAGIC_API_MCP_TOKEN` | MCP 服务访问令牌 | 未启用 |
| `MAGIC_API_OAUTH2_CLIENT_ID` | OAuth2/OIDC 单点登录客户端 ID | 未启用 |
| `MAGIC_API_OAUTH2_CLIENT_SECRET` | OAuth2/OIDC 客户端密钥 | 未启用 |
| `MAGIC_API_RESOURCE_LOCATION` | 数据持久化目录 | `/data/magic-api` |
| `TZ` | 时区 | `Asia/Shanghai` |
| `JAVA_OPTS` | JVM 参数 | 空 |

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
