# Tasks

- [x] Task 1: 修复 MCP token 拦截器未注册问题
  - [x] 1.1 McpAutoConfiguration 实现 WebMvcConfigurer，构造器 @Value 注入 token 与 mcpPath，addInterceptors 注册拦截器（addPathPatterns 用解析后的真实路径——占位符字符串在 addPathPatterns 中不解析，第一版因此失效）
  - [x] 1.2 McpController：删除 handle() 空壳鉴权块；删除构造器 token 参数与 token 字段
- [x] Task 2: 配置文件与文档同步
  - [x] 2.1 application.yml：新增 magic-api.mcp 配置块（enabled + token 占位符 `${MAGIC_API_MCP_TOKEN:}` + 注释，不写明文）
  - [x] 2.2 application-local.yml.example、DEPLOY.md env 示例同步 MAGIC_API_MCP_TOKEN 说明；SKILL.md L20 原有说明已正确无需改
- [x] Task 3: 构建与行为验证
  - [x] 3.1 `mvn -pl magic-api-mcp -am clean package -DskipTests` 构建成功（exit=0）
  - [x] 3.2 9999 实例（local.yml 注入真值 token）验证：no-token → 401 error -32001；bad-bearer → 401；good-bearer → 200 tools/list；X-Mcp-Token → 200 tools/list
  - [x] 3.3 工作台 /magic/web 200 不受影响；空 token 9998 实例 MCP 免鉴权可用（零配置兼容）
- [x] Task 4: 清理验证环境（停 9998 实例、删临时数据目录与根目录 local.yml 副本）

# Task Dependencies
- Task 2、Task 3 依赖 Task 1；2.1 与 2.2 可并行
