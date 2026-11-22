# Tasks

- [x] Task 1: 外置凭据
  - [x] 1.1 application.yml：L28-29/L34-35 四项凭据改为 `${MAGIC_API_SECURITY_USERNAME:}` 等 4 个环境变量占位符（默认空）；头部注释补凭据注入说明
  - [x] 1.2 新增 application-local.yml.example（入库模板，4 键 + 注释）
  - [x] 1.3 新增 application-local.yml（本地真值：admin/123456 + 当前 client-id/secret），填入后本机可用
  - [x] 1.4 magic-api-server/.gitignore 与根 .gitignore 追加 application-local.yml
- [x] Task 2: 文档同步
  - [x] 2.1 DEPLOY.md 新增「敏感配置管理」章节：EnvironmentFile 方式（chmod 600）+ local.yml 方式 + 轮换警告；unit 示例补 EnvironmentFile 行
- [x] Task 3: 验证
  - [x] 3.1 `git grep` 确认跟踪文件无明文凭据；`git status`/`check-ignore` 确认 application-local.yml 被忽略
  - [x] 3.2 重新打包，以根目录为工作目录启动（application-local.yml 不在根目录 → 需验证占位符为空时启动不报错；OIDC 配置经环境变量或复制 local.yml 到工作目录验证一次完整加载），/magic/web 200

# Task Dependencies
- Task 3 依赖 Task 1；Task 2 独立可并行
