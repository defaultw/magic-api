# GitHub Actions 自动发布 Docker 镜像 Spec

## Why
镜像目前只能在本地 `docker build`。需要在 GitHub 仓库中建立 CI 自动化：发布 release 时自动构建镜像并推送到 Docker Hub（版本与 release 一致，同时更新 latest），并支持手动触发指定版本，使交付流程标准化。

## What Changes
- 新增 `.github/workflows/docker-release.yml`（单一 workflow，双触发）：
  - `release: published` 触发：版本号取自 release tag（去掉可选 `v` 前缀，如 `v2.2.2` → `2.2.2`），推送 `defaultw/magic-api-server:<version>` 与 `defaultw/magic-api-server:latest`
  - `workflow_dispatch` 触发：输入 `version`（必填，如 `2.2.2` 或 `v2.2.2`，自动归一化去 `v`）与 `push_latest`（布尔，默认 true，控制是否同时发布 latest）
  - 构建命令与本地一致：`docker build -f magic-api-server/Dockerfile -t <tags> .`（上下文 = 仓库根，自动读取 `magic-api-server/Dockerfile.dockerignore`）
  - 推送凭据经 GitHub Secrets：`DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`
  - Manifest（docker/metadata-action）按 tag 规则生成，`latest` 仅在非手动覆盖或 `push_latest=true` 时附加
  - BuildKit 缓存（`cache-from`/`cache-to` type=gha）加速后续构建
  - 保存/上传构建日志与镜像 digest 作为 workflow artifact，便于排障与审计
- 更新 `magic-api-server/DEPLOY-DOCKER.md`：新增"从 Docker Hub 拉取官方镜像"小节（拉取命令、tag 说明、Secrets 配置指引、手动触发指引）

## Impact
- Affected specs: `dockerize-magic-api-server`（已完成，提供 Dockerfile/dockerignore 基础设施，本次消费不修改）
- Affected code:
  - 新增：`.github/workflows/docker-release.yml`
  - 修改：`magic-api-server/DEPLOY-DOCKER.md`（追加 Docker Hub 拉取小节）
  - 不改动 Dockerfile、compose、Java 代码

## ADDED Requirements

### Requirement: Release 触发自动发布
当 GitHub Release 被发布（published）时，系统 SHALL 自动构建镜像并推送 `docker.io/defaultw/magic-api-server:<version>`（版本与 release tag 一致，兼容 `v` 前缀）及 `docker.io/defaultw/magic-api-server:latest`。

#### Scenario: 发布 v2.2.3 release
- **WHEN** 发布 tag 为 `v2.2.3` 的 release
- **THEN** workflow 自动触发，Docker Hub 出现 `defaultw/magic-api-server:2.2.3` 与 `defaultw/magic-api-server:latest`
- **AND** latest 指向与 2.2.3 相同镜像 digest

#### Scenario: 无 v 前缀的 release tag
- **WHEN** 发布 tag 为 `2.2.3`（无 v 前缀）的 release
- **THEN** 同样产出 `2.2.3` 与 `latest`（归一化逻辑对两种形式均生效）

### Requirement: 手动触发指定版本
用户 SHALL 能通过 workflow_dispatch 手动触发，指定要构建推送的版本号；可选控制是否附加 latest。

#### Scenario: 手动构建指定版本
- **WHEN** 在 Actions 页手动触发并输入 `version=2.2.2`、`push_latest=false`
- **THEN** 仅推送 `defaultw/magic-api-server:2.2.2`，latest 不变

#### Scenario: 手动触发缺省 latest 行为
- **WHEN** 手动触发未改 push_latest（默认 true）
- **THEN** 推送 `<version>` 与 `latest`，与 release 触发行为一致

### Requirement: 凭据安全
workflow SHALL 仅使用 GitHub Secrets（`DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN`）登录 Docker Hub，任何凭据不落日志。

#### Scenario: Secrets 未配置时的行为
- **WHEN** Secrets 未配置时触发 workflow
- **THEN** 构建步骤成功完成，登录/推送步骤明确失败并提示需配置 Secrets

### Requirement: 构建可观测
workflow SHALL 上传构建日志与镜像 digest 为 artifact，并在失败时保留供排障。

#### Scenario: 排障
- **WHEN** workflow 失败
- **THEN** 可从 run 页面下载日志 artifact 与 digest 文件定位问题

### Requirement: 文档同步
DEPLOY-DOCKER.md SHALL 追加 Docker Hub 拉取指引：镜像名与 tag 规则、docker pull/run 示例、Secrets 配置步骤、手动触发步骤。

#### Scenario: 依文档完成首次 CI 发布
- **WHEN** 使用者按文档配置 Secrets 后发布 release 或手动触发
- **THEN** 无需询问即可完成镜像发布流程
