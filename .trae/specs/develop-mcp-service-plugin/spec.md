# Magic-API MCP 服务模块 Spec

## Why
magic-api 目前只能通过 Web 编辑器或 HTTP 管理端点操作，智能体（LLM Agent）无法以标准化 MCP（Model Context Protocol）方式直接管理接口。需要一个即插即用的 MCP 服务模块：引入 demo 工程后零配置即可用，并配套标准 SKILLS 配置文件供任意智能体调用。

## What Changes
- 新增 Maven 模块 `magic-api-mcp`（Java 8 / Spring Boot 2.4.5 兼容）：
  - 内嵌 MCP Streamable-HTTP JSON-RPC 2.0 服务端，默认端点 `POST /mcp/magic`（路径可配 `magic-api.mcp.path`，默认值即可用，无需额外配置）
  - 通过 `spring.factories` 自动装配，依赖 starter 已有的 `MagicResourceService` / `MagicAPIService` / `MagicConfiguration` 等 Bean，在进程内直接调用（不回环走 HTTP，不受编辑器登录鉴权/OIDC 影响）
  - 可选鉴权：`magic-api.mcp.token` 非空时校验请求头，默认空 = 开放（保证即插即用）
  - 实现全量工具（与官方 Web 端点逻辑对齐，见 ADDED Requirements）
- 父 `pom.xml`：`<modules>` 增加 `magic-api-mcp`
- demo 工程集成：`magic-api-demo/pom.xml` 增加 `magic-api-mcp` 依赖；**不改动** `application.yml`（零配置验证）
- 生成标准 SKILLS 配置文件：`magic-api-mcp/skills/magic-api/SKILL.md`（含 frontmatter name/description、MCP 接入方式、全部工具目录+JSON Schema+调用示例、官方文档全量收录）
  - 官方文档遍历：从 `https://www.ssssssss.org/magic-api/pages/quick/intro/` 出发，沿左侧菜单/页尾"下一篇"链接遍历全部页面（指南/语法/进阶配置/插件等），逐页抓取并收录核心内容到 SKILL.md
- 输出使用说明文档：`magic-api-mcp/USAGE.md`（模块引入方式、端点协议、工具清单、调试方法、常见问题）
- 功能自测：打包 demo 后启动，用 JSON-RPC 报文对全部工具做真实验证

## Impact
- Affected specs: 无既有 spec（新增能力）
- Affected code:
  - 新增：`magic-api-mcp/**`（模块全部代码）
  - 修改：`pom.xml`（父模块注册）、`magic-api-demo/pom.xml`（依赖引入）
  - 不修改：`magic-api` 核心源码、`magic-api-spring-boot-starter`、`magic-api-demo/src/main/resources/application.yml`

## ADDED Requirements

### Requirement: MCP 传输与协议端点
系统 SHALL 在应用内提供 MCP Streamable-HTTP 端点，实现 JSON-RPC 2.0：
- `initialize` / `notifications/initialized` / `ping` / `tools/list` / `tools/call`，握手失败或未知方法返回标准 JSON-RPC error
- 会话管理：initialize 时下发 `Mcp-Session-Id` 响应头，后续请求校验
- `tools/call` 返回 MCP content 结构（`type: text`，JSON 字符串化结果），工具内部异常转为 `isError: true` 的文本结果而非 500

#### Scenario: 零配置握手成功
- **WHEN** demo 引入依赖并启动，客户端 POST initialize 到 `/mcp/magic`
- **THEN** 返回 `protocolVersion`、`capabilities.tools`、`serverInfo`，且响应带 `Mcp-Session-Id`

#### Scenario: 鉴权开关
- **WHEN** 配置 `magic-api.mcp.token` 非空且请求未携带匹配令牌
- **THEN** 返回 401 JSON-RPC error；token 默认为空时不做任何校验

### Requirement: 分组管理工具（对齐官方分组功能）
- `magic_group_tree(type?)` → `MagicResourceService.tree()` 分组树
- `magic_group_get(id)` → `getGroup` 详情
- `magic_group_save(id?, parentId?, name, path, ...)` → `saveGroup` 新增/编辑分组
- `magic_group_delete(id)` → `delete` 删除分组
- `magic_group_move(src, groupId)` → `move`
- `magic_group_copy(src, target)` → `copyGroup`

#### Scenario: 新增分组
- **WHEN** 调用 `magic_group_save` 传入 name/path/parentId
- **THEN** 返回新分组 id，`magic_group_tree` 中可见该节点

### Requirement: 接口管理工具（对齐官方接口 CRUD）
- `magic_api_list(groupId)` → `listFiles` 分组下接口列表
- `magic_api_get(id)` → `file` 接口详情（含 script/parameters/headers/options/paths/responseBody 等）
- `magic_api_save(id?, groupId, name, path, method, script, parameters[], headers[], options[], paths[], requestBody?, responseBody?, description?, ...)` → `saveFile`，参数配置与响应设置通过上述字段完整承载
- `magic_api_delete(id)` → `delete`
- `magic_api_move(src, groupId)` → `move`
- `magic_api_lock(id)` / `magic_api_unlock(id)` → `lock`/`unlock`
- `magic_api_search(keyword)` → 遍历 `files()` 按名称/路径过滤

#### Scenario: 新增并编辑接口
- **WHEN** 调用 `magic_api_save`（无 id）创建，再带 id 修改 script
- **THEN** 两次均返回成功，`magic_api_get` 内容与最后一次保存一致

### Requirement: 接口执行与调试工具（对齐官方调试功能）
- `magic_api_execute(method, path, context?)` → `MagicAPIService.call` 执行已发布接口；无 HTTP 请求上下文时降级 `execute`
- `magic_api_debug(...)` → 与编辑器"调试"语义对齐：临时构建/注册 ApiInfo → 执行 → 注销，支持传入完整脚本与测试参数、请求头，返回执行结果

#### Scenario: 执行已发布接口
- **WHEN** 已保存 `GET /demo/test` 接口后调用 `magic_api_execute`
- **THEN** 返回脚本执行结果（含 code/message 或原始结果）

#### Scenario: 调试未保存脚本
- **WHEN** 调用 `magic_api_debug` 传入临时脚本
- **THEN** 脚本被执行并返回结果，且资源树中不留残留接口

### Requirement: 辅助管理工具（对齐官方其余功能）
- `magic_resource_tree` → 全量资源树（分组+接口概览）
- `magic_config_get` → 编辑器 `config.json` 配置
- `magic_plugins_list` → 已装插件列表
- `magic_backup_list` / `magic_backup_restore(id)` / `magic_backup_delete(id)` → 备份服务；未配置备份时返回友好错误文本（不抛 500）
- `magic_api_export(groupId?, resources?)` → 导出 zip（base64）
- `magic_api_upload(fileBase64, mode)` → 导入备份包

#### Scenario: 备份工具优雅降级
- **WHEN** demo 未配置备份且调用 `magic_backup_list`
- **THEN** 返回 `isError:false` 的文本说明"备份服务未启用"，HTTP 状态 200

### Requirement: SKILLS 配置文件
系统 SHALL 产出 `magic-api-mcp/skills/magic-api/SKILL.md`：
- frontmatter：`name`、`description`
- 正文：模块定位、MCP 接入步骤（HTTP JSON-RPC 示例）、全部工具的名称/参数 Schema/返回示例/调用示例、注意事项
- 官方文档全量收录：遍历 `https://www.ssssssss.org/magic-api/pages/quick/intro/` 侧边栏全部条目（快速入门、语法、进阶配置、插件、更新说明等），逐页收录要点内容（配置项、语法、示例代码）

#### Scenario: 文档遍历完整性
- **WHEN** 按 SKILL.md 内"官方文档目录"逐项核对官方站点侧边栏
- **THEN** 每个条目在 SKILL.md 中均有对应章节与要点内容

### Requirement: 即插即用集成
- 引入 `magic-api-mcp` 依赖后，无需任何 yml/properties 配置即可完成全部工具调用
- demo 集成后原有功能（编辑器、OIDC 登录、业务接口）不受影响

#### Scenario: 零配置启动
- **WHEN** demo 仅新增依赖并重新打包启动
- **THEN** `/mcp/magic` 可用，且 `application.yml` 无新增配置项

## MODIFIED Requirements
（无）

## REMOVED Requirements
（无）
