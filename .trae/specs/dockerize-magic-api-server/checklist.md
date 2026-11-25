# Checklist

- [x] Dockerfile 存在于 magic-api-server/ 且为多阶段构建（构建阶段含 Maven+Node，运行阶段 JRE8 非 root）
- [x] Dockerfile ENTRYPOINT 强制 `--magic-api.resource.location=/data/magic-api`（可经 `MAGIC_API_RESOURCE_LOCATION` 覆盖）
- [x] 镜像内置 HEALTHCHECK（curl 编辑器 index.html，start-period ≥ 45s，实际 60s）
- [x] Dockerfile.dockerignore 排除 target、node_modules、magic-api-files、.git、.trae、application-local.yml、magic-api.env、magic-api-data
- [x] `docker build -f magic-api-server/Dockerfile -t magic-api-server:2.2.2 .` 构建成功
- [x] docker-compose.yml：端口 9999 映射、`./magic-api-data:/data/magic-api` bind mount、`env_file: magic-api.env (required: false)`、`restart: unless-stopped`、TZ
- [x] `docker compose config` 在有/无 magic-api.env 两种情况下均通过
- [x] magic-api.env.example 提供 5 个凭据变量及生成指引
- [x] 根 .gitignore 已追加 magic-api.env 与 magic-api-data/，且不影响既有条目
- [x] `docker compose up -d` 后容器健康状态达到 healthy
- [x] `curl http://127.0.0.1:9999/magic/web/index.html` 返回 200
- [x] 首次启动后宿主机 magic-api-data/ 生成 api/、function/、datasource/ 子目录（另有 task/）
- [x] down/up 循环后此前创建的数据仍存在（持久化生效）
- [x] 本地开发数据 magic-api-files/ 未进入镜像（`docker run --rm --entrypoint ls <img> /data/magic-api` 输出为空）
- [x] DEPLOY-DOCKER.md 覆盖：构建、运行、持久化与迁移、凭据、验证、OIDC 白名单、备份恢复、运维速查、常见问题，命令可直接复制
- [x] 无凭据文件时容器可启动（OIDC/密码登录不可用但不崩溃）
