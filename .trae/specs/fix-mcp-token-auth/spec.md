# MCP Token 鉴权失效修复 Spec

## Why
用户配置了 `magic-api.mcp.token`，但不带 `Authorization: Bearer <token>` 也能成功调用 MCP。排查发现 token 校验代码存在但从未生效：`magicApiMcpTokenInterceptor` Bean 定义后从未注册到 Spring MVC 拦截器链，McpController 内的「兜底」也只是空壳注释。配置了 token 却不校验属安全缺陷，须修复。

## What Changes
- **BREAKING**（安全修复，行为变更）：配置 `magic-api.mcp.token` 后，MCP 端点强制校验 `X-Mcp-Token` 或 `Authorization: Bearer <token>`；未带 token 的请求返回 401 JSON-RPC 错误
- McpAutoConfiguration 新增 WebMvcConfigurer，将 token 拦截器注册到 MVC 拦截器链，仅拦截 `${magic-api.mcp.path:/mcp/magic}`
- McpController.handle() 删除 L64-67 的空壳鉴权块（「鉴权由拦截器完成，此处兜底」实际无任何逻辑）及未用的 token 字段
- application.yml / DEPLOY.md / SKILL.md 中相关说明同步

## Impact
- Affected specs: develop-mcp-service-plugin
- Affected code:
  - magic-api-mcp/src/main/java/org/ssssssss/magicapi/mcp/McpAutoConfiguration.java（新增 WebMvcConfigurer 注册拦截器）
  - magic-api-mcp/src/main/java/org/ssssssss/magicapi/mcp/McpController.java（删除死代码与未用 token 字段）
  - magic-api-server/src/main/resources/application.yml（补 mcp.token 注释示例/占位符）

## ADDED Requirements
### Requirement: MCP Token 强制校验
当且仅当 `magic-api.mcp.token` 非空时，系统 SHALL 对 MCP 端点（`${magic-api.mcp.path:/mcp/magic}`）的所有请求强制校验 token：
- `X-Mcp-Token` 头优先
- 其次 `Authorization: Bearer <token>`（取第 7 字符起）
- 均缺失或不匹配 → HTTP 401 + JSON-RPC error code -32001「未授权」

#### Scenario: 带正确 Bearer token
- **WHEN** 请求头 `Authorization: Bearer <正确token>` 调用 tools/list
- **THEN** 返回 200 与正常 JSON-RPC 结果

#### Scenario: 不带 token
- **WHEN** 未携带任何 token 头调用 tools/list
- **THEN** 返回 401 JSON-RPC error -32001，请求不会到达 McpController

#### Scenario: token 未配置
- **WHEN** `magic-api.mcp.token` 为空或未配置
- **THEN** 拦截器直接放行，保持零配置可用

#### Scenario: 拦截范围仅限 MCP 路径
- **WHEN** 请求 `/magic/web`（工作台）或其他非 MCP 路径
- **THEN** MCP token 拦截器不参与（addPathPatterns 仅注册 MCP path），由 magic-api 自身鉴权体系负责

## MODIFIED Requirements
### Requirement: McpController 死代码清理
McpController.handle() 中空壳鉴权块 SHALL 删除；构造器与 token 字段一并删除。

#### Scenario: 清理后构建通过
- **WHEN** 执行 `mvn -pl magic-api-mcp -am clean package -DskipTests`
- **THEN** 构建成功，MCP 功能行为不变

## REMOVED Requirements
### Requirement: 「鉴权由拦截器完成，此处兜底」空壳块
**Reason**: 事实上没有任何兜底逻辑，误导排查方向
**Migration**: 删除；鉴权完全由拦截器强制执行
