# Tasks

- [x] Task 1: 新增 `.github/workflows/docker-release.yml`
  - [x] 1.1 双触发定义：`release: types: [published]` + `workflow_dispatch`（inputs：`version` string 必填描述示例 `2.2.2` 或 `v2.2.2`；`push_latest` boolean 默认 true）
  - [x] 1.2 版本归一化 step：release 事件取 `github.event.release.tag_name`，dispatch 事件取 `inputs.version`，统一去 `v` 前缀并输出 `VERSION`、`PUSH_LATEST` 与多行 `tags`（heredoc 格式；`push_latest=false` 时 tags 仅含版本 tag）
  - [x] 1.3 构建 step：`docker/build-push-action@v6`，`context: .`、`file: magic-api-server/Dockerfile`、`push: true`、`tags: ${{ steps.ver.outputs.tags }}`；BuildKit gha 缓存（cache-from/cache-to, mode=max）
  - [x] 1.4 推送 step：`docker/login-action@v3` 用 `secrets.DOCKERHUB_USERNAME`/`secrets.DOCKERHUB_TOKEN` 登录，build-push-action push: true 一步完成推送
  - [x] 1.5 可观测 step：`if: always()` 记录 digest（`docker buildx imagetools inspect`，失败标记 push-failed-or-skipped）+ `actions/upload-artifact@v4` 上传 image-digest（保留 30 天）
  - [x] 1.6 验证：actionlint（docker rhysd/actionlint 镜像）0 告警（修复一条 SC2086 后通过）；归一化脚本在 debian 容器内模拟 4 场景（release v2.2.3 / release 2.2.3 / dispatch push_latest=false / dispatch push_latest=true）输出全部正确

- [x] Task 2: 更新 DEPLOY-DOCKER.md
  - [x] 2.1 已有第三节"从 Docker Hub 拉取镜像（推荐）"（本轮 spec 之前已存在）：tag 规则表、pull 命令、bash/PowerShell 完整 run 示例（含 magic-api-data 挂载与 env 文件）、CI 镜像 vs 本地构建选择——本次补全其引用的"第十四节"
  - [x] 2.2 新增第十四节"CI 自动发布镜像（GitHub Actions)"：两种触发方式表格（release 自动 / Actions 页手动 Run workflow 填 version 与 push_latest）、Secrets 一次性配置步骤（Docker Hub Access Token 生成 → 仓库 Settings → Secrets 添加两条）、gh release create 发布示例、image-digest artifact 排障说明、CI vs 本地构建对比
  - [x] 2.3 验证：文档镜像名（defaultw/magic-api-server）、tag 规则（去 v 前缀 + latest 行为）与 workflow 实现逐字一致

# Task Dependencies
- Task 2 依赖 Task 1（文档描述须与 workflow 最终实现一致）
