# magic-api-mcp 使用说明

magic-api MCP 服务模块——将 magic-api 全量操作能力以 MCP（Model Context Protocol）标准协议暴露，任何支持 MCP 的智能体/客户端即可直接管理接口全生命周期：分组管理、接口增删改查、参数配置、响应设置、脚本调试、备份导入导出等。

## 特性

- **即插即用**：引入依赖即自动装配，零配置；端点默认 `/mcp/magic`
- **25 个标准工具**：完整对齐 magic-api 官方功能（分组/接口/执行调试/备份导入导出）
- **进程内调用**：直接调用 `MagicResourceService` / `MagicAPIService` Bean，不回环 HTTP，绕过编辑器鉴权
- **标准协议**：JSON-RPC 2.0 over HTTP，兼容任意 MCP 客户端
- **安全可选**：内置 token 鉴权（默认关闭，`magic-api.mcp.token` 一行开启）
- **不干扰官方编辑器**：与 `/magic/web` 编辑器共存，互不影响

## 模块结构

```
magic-api-mcp/
├── pom.xml                          # Maven 模块定义（parent: magic-api-parent 2.2.2）
├── USAGE.md                         # 本文档
├── skills/magic-api/SKILL.md        # SKILLS 配置文件（供智能体调用）
└── src/main/java/org/ssssssss/magicapi/mcp/
    ├── McpController.java           # MCP 端点控制器（JSON-RPC 分发/会话管理）
    ├── McpProtocol.java             # JSON-RPC 消息构建
    ├── McpToolBuilder.java          # 工具 schema builder
    ├── MagicApiTools.java           # 25 个工具实现（进程内调用）
    └── McpAutoConfiguration.java    # Spring Boot 自动装配（含 token 鉴权拦截器）
```

## 快速开始

### 1. 引入依赖

在任意 Spring Boot Web 工程（Spring Boot 2.x / Java 8+）的 `pom.xml` 中加入：

```xml
<dependency>
    <groupId>org.ssssssss</groupId>
    <artifactId>magic-api-mcp</artifactId>
    <version>2.2.2</version>
</dependency>
```

前提：工程已引入 `magic-api-spring-boot-starter`（MCP 模块复用其自动装配的 `MagicConfiguration` 等核心 Bean）。

### 2. 启动即用

启动应用，MCP 端点即刻可用，无需任何 MCP 配置：

- **端点**：`POST http://<host>:<port>/mcp/magic`
- **Content-Type**：`application/json; charset=utf-8`

### 3. MCP 客户端接入（JSON-RPC 2.0）

**Step 1 — initialize 握手**

```bash
curl -X POST http://localhost:8080/mcp/magic \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"my-agent","version":"1.0"}}}'
```

响应头 `Mcp-Session-Id` 为会话 ID（后续请求可携带，不携带也可正常调用）。

**Step 2 — tools/list 获取工具清单**

```bash
curl -X POST http://localhost:8080/mcp/magic \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

**Step 3 — tools/call 调用工具**

```bash
curl -X POST http://localhost:8080/mcp/magic \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"magic_group_tree","arguments":{"type":"api"}}}'
```

其他协议方法：`ping`（心跳）、`notifications/initialized`（初始化完成通知）。

### 4. 验证安装

```bash
# tools/list 应返回 25 个工具
curl -s -X POST http://localhost:8080/mcp/magic -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | grep -o '"name":"magic_[a-z_]*"' | wc -l
```

## 配置项（全部可选）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `magic-api.mcp.enabled` | `true` | 是否启用 MCP 服务 |
| `magic-api.mcp.path` | `/mcp/magic` | MCP 端点路径 |
| `magic-api.mcp.token` | 空（开放） | 鉴权 token，配置后请求须携带 `X-Mcp-Token` 头或 `Authorization: Bearer <token>` |

## 工具清单（25 个）

### 分组管理（6 个）

| 工具名 | 说明 | 必填参数 | 可选参数 |
|---|---|---|---|
| `magic_group_tree` | 获取分组树 | - | type（默认 api） |
| `magic_group_get` | 获取分组详情 | id | - |
| `magic_group_save` | 新增/编辑分组 | name | id（编辑）、parentId（默认根）、path、type（默认 api） |
| `magic_group_delete` | 删除分组或文件 | id | - |
| `magic_group_move` | 移动分组/接口到目标分组 | src, groupId | - |
| `magic_group_copy` | 复制分组 | src | target（目标父分组） |

### 接口管理（8 个）

| 工具名 | 说明 | 必填参数 | 可选参数 |
|---|---|---|---|
| `magic_api_list` | 分组下接口列表 | groupId | - |
| `magic_api_get` | 接口详情（脚本/参数/选项/响应设置） | id | - |
| `magic_api_save` | 新增/编辑接口 | groupId, name, path, script | id（编辑）、method（默认 GET）、parameters、headers、paths、options、requestBody、responseBody、description |
| `magic_api_delete` | 删除接口或分组 | id | - |
| `magic_api_move` | 移动接口到目标分组 | src, groupId | - |
| `magic_api_lock` | 锁定接口 | id | - |
| `magic_api_unlock` | 解锁接口 | id | - |
| `magic_api_search` | 按名称/路径搜索 | keyword | - |

### 执行与调试（2 个）

| 工具名 | 说明 | 必填参数 | 可选参数 |
|---|---|---|---|
| `magic_api_execute` | 执行已发布接口 | path | method（默认 GET）、context |
| `magic_api_debug` | 调试脚本（临时注册，不落盘） | script | groupId、name、path、method、parameters、headers、pathVariables、body |

### 辅助管理（9 个）

| 工具名 | 说明 | 必填参数 | 可选参数 |
|---|---|---|---|
| `magic_resource_tree` | 全量资源树（所有分组类型） | - | - |
| `magic_config_get` | 运行配置 | - | - |
| `magic_plugins_list` | 已安装插件 | - | - |
| `magic_backup_list` | 查询备份记录 | - | timestamp |
| `magic_backup_restore` | 还原备份 | id | timestamp |
| `magic_backup_delete` | 删除备份 | id | - |
| `magic_backup_full` | 执行全量备份 | - | - |
| `magic_api_export` | 导出接口包（zip→base64） | - | groupId、resources |
| `magic_api_upload` | 导入接口包（base64→zip） | fileBase64 | mode（full/increment，默认 full） |

> 备份类工具依赖 `magic-api.resource.backup` 配置；未启用时返回说明文本，不会报错。

## 调用示例

### 新增分组 + 新增接口（完整参数配置）

```json
// 1. magic_group_save
{"name": "订单管理", "parentId": "0", "path": "order"}
// 返回 {"id":"<分组ID>","name":"订单管理",...}

// 2. magic_api_save（参数配置 + 响应设置）
{
  "groupId": "<分组ID>",
  "name": "订单详情",
  "path": "/detail",
  "method": "POST",
  "script": "return db.table('t_order').selectById(id)",
  "parameters": [{"name":"id","value":"","description":"订单ID","required":true,"dataType":"String"}],
  "headers": [{"name":"X-Token","value":"","description":"鉴权令牌"}],
  "options": [{"name":"anonymous","value":false}],
  "requestBody": "{\"id\":\"订单ID\"}",
  "responseBody": "{\"code\":1,\"data\":{...}}",
  "description": "查询订单详情"
}
// 返回接口 ID

// 3. magic_api_execute 执行
{"method": "POST", "path": "order/detail", "context": {"id": "123"}}
```

### 调试脚本（不落盘）

```json
// magic_api_debug
{
  "script": "return 42",
  "method": "POST",
  "body": {"a": 1},
  "parameters": {"b": "x"}
}
// 返回 42，不产生任何持久化文件
```

### 移动接口 / 分组

```json
// magic_api_move：把接口移到目标分组
{"src": "<接口ID>", "groupId": "<目标分组ID>"}
// magic_group_move：把分组移到目标分组下（成为子分组）
{"src": "<源分组ID>", "groupId": "<目标分组ID>"}
```

### 导出/导入

```json
// magic_api_export → base64 zip
{"groupId": "<分组ID>"}
// magic_api_upload
{"fileBase64": "<export 返回的 base64>", "mode": "full"}
```

## 响应格式

**成功**：

```json
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"<结果 JSON 或文本>"}],"isError":false}}
```

**工具业务错误**（如参数缺失、资源不存在）：HTTP 200，`isError:true`，text 为错误说明：

```json
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"缺少必填参数: id"}],"isError":true}}
```

**未知工具**：JSON-RPC error：

```json
{"jsonrpc":"2.0","id":1,"error":{"code":-32602,"message":"未知工具: xxx"}}
```

## 安全建议

- 生产环境务必配置 `magic-api.mcp.token`，并使用 HTTPS
- MCP 端点拥有接口全量写权限，建议仅在内网或受信环境开放
- token 通过请求头传递：`X-Mcp-Token: <token>` 或 `Authorization: Bearer <token>`

## 常见问题

**Q: 引入依赖后启动报 `MagicConfiguration` 相关 Bean 缺失？**
A: 确认工程同时引入了 `magic-api-spring-boot-starter` 且为 Web 应用（`spring-boot-starter-web`）。

**Q: 保存分组报"不支持该分组类型"？**
A: `magic_group_save` 已默认 `type=api`；若显式传 type，须为 storages 中注册的类型（api/function/function-class/datasource/columns）。

**Q: 保存接口报"该路径已被使用"？**
A: magic-api 全局路径唯一，换个 path 或确认是否为编辑（传 `id`）。

**Q: 备份工具返回"备份服务未启用"？**
A: 在 application.yml 配置 `magic-api.resource.backup: enabled`（或自定义 `MagicBackupService` Bean）后重启。

**Q: 如何与官方编辑器共存？**
A: 两者共享同一套存储与注册机制，MCP 的修改实时同步到编辑器，反之亦然；互不冲突。
