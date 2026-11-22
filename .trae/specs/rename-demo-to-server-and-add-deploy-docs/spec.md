# 重命名 demo 工程为 server 并编写 systemd 部署文档 Spec

## Why

用户需要：①明确 magic-api 接口数据的存储位置（开发机现状 + Ubuntu 部署后）及备份方法；②一份可直接落地的 systemd 部署方案文档放到 demo 工程；③工程名 `magic-api-demo` 改为 `magic-api-server`（反映其作为可部署服务端的定位，不再是 demo）。

**数据存储现状（分析结论，写入文档）**：
- 存储引擎：`magic-api.resource.type: file`，位置 `location: ./magic-api-files/`（[application.yml](file:///d:/project/magic-api/magic-api-demo/src/main/resources/application.yml) 第 25 行）
- **相对路径语义**：实际目录 = JVM 启动时工作目录 + `magic-api-files/`，与 jar 文件所在目录无关
- 当前 9999 实例工作目录为 `d:\project\magic-api`，故数据实际存于工作区根 `magic-api-files\`（TEst 分组等）；`magic-api-demo\magic-api-files\` 为自带初始数据（demo 分组），未被当前实例使用
- Ubuntu 部署后：数据存于 systemd `WorkingDirectory` 下的 `magic-api-files/`；生产建议用启动参数覆盖为绝对路径（如 `--magic-api.resource.location=/var/lib/magic-api`），或固定 WorkingDirectory，二者文档中均给出
- 备份方式（按优先级写入文档）：①文件存储直接 tar/cp 打包数据目录；②UI/MCP 全量导出 zip（增量/全量上传可恢复）；③可选启用 `magic-api.backup`（仅支持存数据库表 magic_api_backup，demo 未配数据源，默认关闭）
- 注意：`location` 以 `classpath:` 开头为只读模式，生产禁用

## What Changes

- **BREAKING**：工程改名 `magic-api-demo` → `magic-api-server`
  - 目录 `magic-api-demo/` → `magic-api-server/`（含 `magic-api-files/` 初始数据、源码、.gitignore 随目录迁移）
  - 父 [pom.xml](file:///d:/project/magic-api/pom.xml) `<module>magic-api-demo</module>` → `<module>magic-api-server</module>`
  - 子 pom：`artifactId`/`name` 改为 `magic-api-server`；`DemoApplication` 类名与包名 `org.ssssssss.magicapi.demo` 保留不动（用户仅要求改工程名）
  - `application.yml` 头部注释中的目录名/启动命令/jar 名同步更新（jar 产物名随之变为 `magic-api-server-2.2.2.jar`，启动命令路径变化）
  - 清理改名前残留的 `target/` 旧产物
- 新增 [magic-api-server/DEPLOY.md](file:///d:/project/magic-api/magic-api-server/DEPLOY.md)：systemd 部署方案文档
  - 数据存储位置说明（开发机现状/Ubuntu 现状/绝对路径覆盖方案）+ 备份方案（tar 脚本 + systemd timer 示例 + UI 导出说明）
  - `magic-api-server.service` unit 文件完整内容（Java 8 运行时要求、WorkingDirectory、ExecStart、自启、内存参数、日志）
  - 部署步骤：上传 jar → 建数据目录 → 安装 unit → 首次启动/自启验证 → 防火墙端口
- 不改动：magic-api 核心源码、magic-api-mcp、其他模块；不迁移数据目录、不改 `resource.location` 配置值（文档仅建议生产覆盖方式）

## Impact

- Affected specs: 无（独立工程重命名 + 文档新增）
- Affected code: `pom.xml`（父模块注册）、`magic-api-server/pom.xml`、`magic-api-server/src/main/resources/application.yml`（仅注释）、新增 `magic-api-server/DEPLOY.md`
- 风险：改名后旧文档/旧 spec 中 `magic-api-demo` 提法成为历史记录（历史 spec 不回改）；本工作区 `magic-api-files\`（根目录实际数据）不受影响

## ADDED Requirements

### Requirement: 工程名为 magic-api-server

Maven 模块 SHALL 命名为 `magic-api-server`（目录名/模块注册/artifactId/name 一致），构建产物为 `magic-api-server-2.2.2.jar`，应用功能与改名前完全一致。

#### Scenario: 改名后构建与启动正常

- **WHEN** 执行 `mvn -pl magic-api-server -am clean package -DskipTests` 后运行 `java -jar magic-api-server/target/magic-api-server-2.2.2.jar`
- **THEN** BUILD SUCCESS；9999 端口启动成功，`/magic/web` 返回 200，既有接口数据正常加载

### Requirement: systemd 部署方案文档

`magic-api-server/DEPLOY.md` SHALL 提供：数据存储位置与备份说明、完整可复制的 `magic-api-server.service` unit 内容、Ubuntu 上的分步部署命令、备份脚本与 systemd timer 示例。

#### Scenario: 文档内容可照做部署

- **WHEN** 按文档步骤在 Ubuntu 上操作（上传 jar、安装 unit、启动）
- **THEN** 服务 `systemctl status` 为 active，数据目录落在文档说明的位置，备份脚本可执行并产出备份包

#### Scenario: 数据存储问题有明确答案

- **WHEN** 阅读 DEPLOY.md 的「数据存储」章节
- **THEN** 能明确得知：当前数据存哪里（相对路径 = 工作目录）、Ubuntu 部署后存哪里（WorkingDirectory 或绝对路径覆盖）、三种备份方法及推荐做法
