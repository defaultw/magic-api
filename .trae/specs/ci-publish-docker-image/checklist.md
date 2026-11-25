# Checklist

- [x] workflow 文件位于 `.github/workflows/docker-release.yml`，YAML 语法有效（actionlint 0 告警通过）
- [x] 双触发齐全：`release: [published]` 与 `workflow_dispatch`（含 version 必填 input 与 push_latest boolean 默认 true）
- [x] release tag 与 dispatch version 均自动去 `v` 前缀归一化（`${RAW_VERSION#v}`，容器内模拟 4 场景验证：v2.2.3/2.2.3/v2.2.2/2.2.2 全部正确）
- [x] release 触发推送 `<version>` 与 `latest` 两个 tag（release 事件 PUSH_LATEST 强制 true）
- [x] `push_latest=false` 手动触发时仅推送 `<version>`，latest 不更新（模拟验证 tags 仅含版本 tag）
- [x] 构建上下文为仓库根、Dockerfile 路径 `magic-api-server/Dockerfile`，与本地构建等价（context: .、file: magic-api-server/Dockerfile）
- [x] 登录仅用 Secrets（DOCKERHUB_USERNAME/DOCKERHUB_TOKEN），凭据不落日志（login-action 标准遮蔽）
- [x] 配置 BuildKit gha 缓存（cache-from: type=gha、cache-to: type=gha,mode=max）
- [x] 构建产物 digest 上传为 artifact（if: always() 双步骤：记录 digest + upload-artifact，保留 30 天）
- [x] DEPLOY-DOCKER.md 含拉取镜像小节（第三节：tag 规则表 + bash/PowerShell pull/run 示例）与 Secrets/手动触发配置指引（新增第十四节：Secrets 生成步骤、release/手动触发路径、gh CLI 发布示例、CI vs 本地构建选择）
- [x] 文档镜像名/tag 规则与 workflow 实现一致（均为 defaultw/magic-api-server，去 v 前缀 + latest 规则一致）
