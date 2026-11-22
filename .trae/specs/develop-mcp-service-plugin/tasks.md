# Tasks

- [x] Task 1: 创建 `magic-api-mcp` Maven 模块骨架
  - [x] 1.1 新建 `magic-api-mcp/pom.xml`（parent=magic-api-parent 2.2.2，依赖 magic-api + spring-boot-starter-web，Java 8）
  - [x] 1.2 父 `pom.xml` `<modules>` 注册 `magic-api-mcp`
- [x] Task 2: 实现 MCP 协议层（JSON-RPC 2.0 / Streamable-HTTP）
  - [x] 2.1 `McpProtocol`：initialize / ping / tools/list / tools/call 分发与错误响应
  - [x] 2.2 `McpController`：`POST {magic-api.mcp.path}`（默认 `/mcp/magic`）+ `Mcp-Session-Id` 会话管理 + 可选 token 鉴权
  - [x] 2.3 `McpAutoConfiguration`（spring.factories 自动装配，零配置可用）
- [x] Task 3: 实现全量 MCP 工具（进程内调用 MagicResourceService/MagicAPIService）
  - [x] 3.1 分组工具：tree/get/save/delete/move/copy
  - [x] 3.2 接口工具：list/get/save/delete/move/lock/unlock/search（字段完整承载 parameters/headers/options/paths/responseBody）
  - [x] 3.3 执行与调试工具：execute（MagicAPIService.call，降级 execute）、debug（临时注册→执行→注销）
  - [x] 3.4 辅助工具：resource_tree/config_get/plugins_list/backup_list/backup_restore/backup_delete/export/upload（备份未启用时优雅降级）
- [x] Task 4: demo 集成（零配置）
  - [x] 4.1 `magic-api-demo/pom.xml` 添加 magic-api-mcp 依赖；不改 application.yml
- [x] Task 5: 生成 SKILLS 配置文件 `magic-api-mcp/skills/magic-api/SKILL.md`
  - [x] 5.1 遍历官方文档全站页面（intro/start/param/crud/语法/进阶/插件等侧边栏全部条目）并整理要点（70/70 页，笔记见 .trae/tmp/magic-api-docs-notes.md）
  - [x] 5.2 编写 SKILL.md：frontmatter + MCP 接入步骤 + 全部工具 Schema/示例 + 官方文档收录章节
- [x] Task 6: 输出使用说明 `magic-api-mcp/USAGE.md`
- [x] Task 7: 功能自测
  - [x] 7.1 `mvn install/package` 打包（demo jar 内嵌最新 magic-api-mcp 已验证）
  - [x] 7.2 启动 demo jar，JSON-RPC 逐一验证：initialize→tools/list→分组/接口/执行/调试/辅助工具全部通过（25 个工具含 move 全链路实测）
  - [x] 7.3 确认编辑器/OIDC 原功能不受影响（编辑器页面 200）

# Task Dependencies
- Task 2、3 依赖 Task 1；Task 4、5、6 依赖 Task 3（工具清单确定后写文档）；Task 7 依赖 Task 4
- Task 5.1 文档遍历可与 Task 2/3 并行
