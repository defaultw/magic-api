# Tasks

- [x] Task 1: 新增 Dockerfile 与 dockerignore（源码多阶段构建）
  - [x] 1.1 在 `magic-api-server/` 新建 `Dockerfile`（实际实现与原计划有两处经构建验证的修正：①基础镜像改为 `maven:3.9-eclipse-temurin-17`——依赖闭包中 magic-api-servlet-jakarta 模块强制 Java 17 编译，JDK8 构建失败，业务模块仍编译为 Java 8 字节码；②去掉 apt-get 安装 curl——两个基础镜像均内置 curl，规避 Debian security 源 404/502）：构建阶段 Maven+Node 双工具链（Node v16.20.2 从 npmmirror 下载），`mvn -B -ntp -pl magic-api-server -am package -DskipTests`，构建参数 `MAVEN_MIRROR_URL`/`NPM_REGISTRY`/`NODE_DIST_BASE_URL` 预置国内源；运行阶段 `eclipse-temurin:8-jre-jammy`，uid 10001 用户 magic，jar 位于 `/app/app.jar`，`ENV MAGIC_API_RESOURCE_LOCATION=/data/magic-api`，`ENTRYPOINT` 注入 `--magic-api.resource.location=${MAGIC_API_RESOURCE_LOCATION:-/data/magic-api}`（追加参数可透传），`EXPOSE 9999`，HEALTHCHECK curl `/magic/web/index.html`，start-period=60s
  - [x] 1.2 新建 `magic-api-server/Dockerfile.dockerignore`（上下文=仓库根）：排除 `**/target`、`**/node_modules`、`**/magic-api-files`、`**/magic-api-data`、`**/magic-api.env`、`**/application-local.yml`、`.git`、`.gitignore`、`.trae`、`.idea`、`**/*.iml`、`out`、`bin`
  - [x] 1.3 验证：`docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .` 构建成功（11 个模块全部 SUCCESS，含 plugin-task npm 前端构建）；运行容器以 Java 1.8.0_512 启动（JRE8 运行时正确）；`docker run --rm --entrypoint sh ... java -version` 可用；`--entrypoint ls /data/magic-api` 为空（无开发数据入镜像）

- [x] Task 2: 新增 docker-compose.yml 与凭据模板
  - [x] 2.1 新建 `magic-api-server/docker-compose.yml`：`build` 段（context `..`、dockerfile `magic-api-server/Dockerfile`、国内加速 args）+ `image: magic-api-server:2.2.2`；`ports: "9999:9999"`；`volumes: ./magic-api-data:/data/magic-api`（bind mount 暴露到宿主机）+ 注释形式的可选 `./application-local.yml:/app/application-local.yml:ro`；`env_file: magic-api.env`（`required: false`）；`restart: unless-stopped`；`TZ=Asia/Shanghai`；healthcheck 沿用镜像内置
  - [x] 2.2 新建 `magic-api-server/magic-api.env.example`：`MAGIC_API_SECURITY_USERNAME/PASSWORD`、`MAGIC_API_OAUTH2_CLIENT_ID/CLIENT_SECRET`、`MAGIC_API_MCP_TOKEN`，含生成指引注释（含 password-login-enabled 默认关闭的提示）
  - [x] 2.3 更新根 `.gitignore`：追加 `magic-api-server/magic-api.env` 与 `magic-api-server/magic-api-data/`，既有条目未动
  - [x] 2.4 验证：`docker compose config` 在有/无 `magic-api.env` 两种情况下均退出码 0（required: false 生效）；`docker compose config --services` 输出 magic-api-server

- [x] Task 3: 容器功能验证（持久化 + 健康 + 凭据）
  - [x] 3.1 无凭据文件场景 `docker compose up -d` 启动成功，`docker inspect --format "{{.State.Health.Status}}"` 最终为 `healthy`；填入凭据重建容器后同样 `healthy`
  - [x] 3.2 `curl http://127.0.0.1:9999/magic/web/index.html` 返回 200
  - [x] 3.3 宿主机 `magic-api-data/` 首次启动自动生成 `api/`、`datasource/`、`function/`、`task/` 子目录；`docker compose down`（目录保留）→ `up -d` 数据仍在（持久化验证通过）；应用日志确认工作目录 `file:///data/magic-api`
  - [x] 3.4 临时 `magic-api.env` 注入后容器内 `MAGIC_API_SECURITY_USERNAME=admin`、`MAGIC_API_MCP_TOKEN=testtoken-abcdef`、`TZ=Asia/Shanghai` 全部生效（容器内 `date` 输出 CST 正确）；验证后测试 env 与测试数据目录已清理，容器已 down
  - 附注：验证期间发现 ENTRYPOINT 对 `docker run image <cmd>` 直接覆盖不生效（sh -c 包装），已在 Dockerfile 注释中写明正确用法（`--entrypoint sh ... -c "..."` 或追加 Spring Boot 参数如 `--server.port=8080`，后者已实测透传成功）

- [x] Task 4: 撰写 DEPLOY-DOCKER.md 详细操作文档
  - [x] 4.1 覆盖 13 章节：简介与 DEPLOY.md 关系、前提条件、快速开始、镜像构建详解（含构建上下文解释与 JDK17 构建原因）、运行详解、持久化目录与三类数据迁移路径、凭据注入（env 文件/application-local.yml/config 覆盖三法）、启动验证清单、OIDC 回调白名单同步（TOPIAM）、备份与恢复、版本升级、日常运维速查表、7 条常见问题 FAQ
  - [x] 4.2 全文 431 行，命令可直接复制执行（PowerShell 与 bash 差异处分开标注）；与既有 DEPLOY.md 互相链接；文档中镜像结构描述已按实际实现校正（maven:3.9-eclipse-temurin-17、镜像内置 curl 等）

# Task Dependencies
- Task 2 依赖 Task 1（compose build 引用 Dockerfile）
- Task 3 依赖 Task 1 + Task 2
- Task 4 依赖 Task 1~3（文档内容需与最终实现一致，可先起草后按验证结果校正）
