# Checklist

- [x] 配置 magic-api.mcp.token 后，MCP 端点无 token 请求返回 401 JSON-RPC error -32001
- [x] Authorization: Bearer <正确token> 调用 tools/list 返回 200 正常结果
- [x] X-Mcp-Token <正确token> 调用 tools/list 返回 200 正确结果
- [x] magic-api.mcp.token 为空时 MCP 端点保持免鉴权可用（9998 实例 --magic-api.mcp.token= 验证 200）
- [x] 拦截器仅拦截 MCP path，/magic/web 工作台鉴权行为不变（200）
- [x] McpController 中空壳鉴权块与未用 token 字段已删除，编译通过
- [x] mvn -pl magic-api-mcp -am clean package -DskipTests 构建成功（exit=0）
- [x] application.yml 中不出现明文 token（token: ${MAGIC_API_MCP_TOKEN:} 占位符，符合凭据外置规范）
