# magic-api-server Docker 镜像化 Spec

## Why

magic-api-server 目前仅有 systemd/裸 jar 部署路径（DEPLOY.md）。需要将其构建为标准 Docker 镜像，并把持久化数据目录以 bind mount 方式暴露到宿主机，使部署、迁移、备份与宿主机文件管理一致可控。

## What Changes

- 新增 `magic-api-server/Dockerfile`：多阶段构建
  - 构建阶段：`maven:3.8.6-openjdk-8` + Node.js（构建闭包含 `magic-api-plugin-task`，其 `generate-resources` 阶段强制执行 `npm install && npm run build`，dist 未入库）；支持 `MAVEN_MIRROR_URL` / `NPM_REGISTRY` 构建参数加速国内网络
  - 运行阶段：`eclipse-temurin:8-jre-jammy`，非 root 用户（magic, uid 10001），安装 curl 用于 HEALTHCHECK，`ENTRYPOINT` 固定 `--magic-api.resource.location=${MAGIC_API_RESOURCE_LOCATION:-/data/magic-api}`，`EXPOSE 9999`
- 新增 `magic-api-server/Dockerfile.dockerignore`（构建上下文为仓库根目录）：排除 `**/target`、`**/node_modules`、`**/magic-api-files`（本地开发数据与示例数据不入镜像）、`.git`、`.trae`、`application-local.yml`、`magic-api.env`、`magic-api-data/` 等
- 新增 `magic-api-server/docker-compose.yml`：
  - 端口 `9999:9999`
  - **持久化暴露到宿主机**：`./magic-api-data:/data/magic-api` bind mount
  - 凭据注入：`env_file: ./magic-api.env`（`required: false`，缺失不阻断启动，仅 OIDC/密码登录不可用）；可选挂载 `./application-local.yml:/app/application-local.yml:ro`
  - `restart: unless-stopped`，`TZ: Asia/Shanghai`
- 新增 `magic-api-server/magic-api.env.example` 凭据模板（与 `application-local.yml.example` 同风格）
- 更新根 `.gitignore`：追加 `magic-api.env`、`magic-api-data/`
- 新增 `magic-api-server/DEPLOY-DOCKER.md` 详细操作文档（用户明确要求）：前提条件、镜像构建（compose build / docker build 两种）、持久化目录说明与数据迁移、凭据注入、启动验证、OIDC 回调白名单注意事项、日常运维（日志/重启/升级/备份恢复）、常见问题

## Impact

- Affected specs: 无既有 spec 覆盖部署形态（`rename-demo-to-server-and-add-deploy-docs` 为 systemd 部署，互补不冲突）
- Affected code:
  - 新增：`magic-api-server/Dockerfile`、`magic-api-server/Dockerfile.dockerignore`、`magic-api-server/docker-compose.yml`、`magic-api-server/magic-api.env.example`、`magic-api-server/DEPLOY-DOCKER.md`
  - 修改：`.gitignore`（根目录）
  - 不改动任何 Java 代码与 `application.yml`

## ADDED Requirements

### Requirement: Docker 镜像可从源码构建

系统 SHALL 提供多阶段 Dockerfile，从仓库源码（构建上下文 = 仓库根目录）构建出可运行的 `magic-api-server` 镜像，镜像内含可执行 Spring Boot jar。

#### Scenario: 全新环境源码构建成功

- **WHEN** 在仓库根目录执行 `docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .`（或 `docker compose build`）
- **THEN** 构建成功产出镜像，无手动预构建 jar 的要求
- **AND** 构建上下文不含 target、node\_modules、本地 magic-api-files 数据

#### Scenario: 国内网络镜像加速

- **WHEN** 通过 build-arg 传入 `MAVEN_MIRROR_URL` / `NPM_REGISTRY`（默认预置国内镜像源）
- **THEN** Maven/npm 依赖下载走指定源

### Requirement: 持久化目录暴露到宿主机

容器内数据目录 `/data/magic-api` SHALL 通过 bind mount 暴露到宿主机（compose 默认 `magic-api-server/magic-api-data/`），应用以 `--magic-api.resource.location=/data/magic-api` 启动。

#### Scenario: 首次启动生成数据

- **WHEN** 容器首次启动
- **THEN** 宿主机 `magic-api-data/` 下出现 `api/`、`function/`、`datasource/` 等子目录
- **AND** 编辑器保存的接口/分组以文件形式落盘到宿主机目录

#### Scenario: 数据跨容器重建持久

- **WHEN** 停止并删除容器（数据目录保留）后重新 `docker compose up -d`
- **THEN** 此前创建的接口与分组仍在编辑器中可见

#### Scenario: 镜像不携带开发数据

- **WHEN** 构建镜像
- **THEN** 本地开发数据（`d:\project\magic-api\magic-api-files\`）与工程示例数据（`magic-api-server/magic-api-files/`）不进入镜像层

### Requirement: 凭据安全注入

敏感凭据 SHALL 仅经环境变量（`magic-api.env`，compose `env_file`）或可选挂载的 `application-local.yml` 注入容器，任何凭据文件不进入镜像与 Git。

#### Scenario: 无凭据文件也能启动

- **WHEN** `magic-api.env` 与 `application-local.yml` 均不存在时 `docker compose up -d`
- **THEN** 容器正常启动，服务可访问（OIDC/密码登录不可用）

#### Scenario: 凭据生效

- **WHEN** 按 `magic-api.env.example` 填写并 `cp` 为 `magic-api.env` 后重启容器
- **THEN** 编辑器密码登录与 OIDC 登录可用、MCP token 鉴权生效

### Requirement: 容器可观测与健康检查

镜像 SHALL 以非 root 用户运行、`EXPOSE 9999`，并内置基于 `curl http://127.0.0.1:9999/magic/web/index.html` 的 HEALTHCHECK（`start-period` 覆盖 15\~25s 启动耗时）。

#### Scenario: 健康检查通过

- **WHEN** 容器启动完成后执行 `docker ps` / `docker inspect --format='{{.State.Health.Status}}'`
- **THEN** 健康状态为 `healthy`

### Requirement: 详细操作文档

SHALL 提供 `magic-api-server/DEPLOY-DOCKER.md`，覆盖：构建、运行、持久化与数据迁移、凭据注入、验证、OIDC 回调白名单同步、备份恢复、日常运维与常见问题，命令均可直接复制执行。

#### Scenario: 依文档可独立完成部署

- **WHEN** 使用者仅依照 DEPLOY-DOCKER.md 操作
- **THEN** 可完成从构建到验证的完整流程，无需查阅其他文档
