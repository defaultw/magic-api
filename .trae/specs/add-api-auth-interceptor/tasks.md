# Tasks

- [x] Task 1: 新增 RequireLoginRequestInterceptor 实现
  - [x] SubTask 1.1: 在 magic-api-server 工程创建 `org.ssssssss.magicapi.demo.RequireLoginRequestInterceptor`（@Component，实现 RequestInterceptor）
  - [x] SubTask 1.2: preHandle 实现：require_login 选项判断 → getUserByToken 校验 Magic-Token → 失败返回 JsonBean(401, "用户未登录")，成功返回 null
- [x] Task 2: 编译打包并启动验证
  - [x] SubTask 2.1: `mvn -pl magic-api-server -am package` 编译通过
  - [x] SubTask 2.2: 启动 jar，创建/复用测试接口并配置 require_login 选项
  - [x] SubTask 2.3: 验证 4 场景：无 token→401、伪造 token→401、密码登录 token→200、未开启选项接口→匿名放行
- [x] Task 3: 勾选 tasks.md / checklist.md 完成项

# Task Dependencies
- Task 2 依赖 Task 1；Task 3 依赖 Task 2

# 验证结果补充
- 拦截器已被 starter 收集：启动日志 `注册请求拦截器：class org.ssssssss.magicapi.demo.RequireLoginRequestInterceptor`
- 密码登录验证需临时开启 `password-login-enabled: true`（当前配置为 false 仅 OIDC），验证后已恢复
- 登出后旧 token 访问 → 401（会话真实失效）
- OIDC authorize 入口因 IdP 侧发现端点 500（iam.xiaojiang.wang 应用配置问题，与本次改动无关）无法端到端实测；代码层面 OIDC 与密码登录共用同一会话签发/校验入口（createSession → getUserByToken），已被密码登录链路验证覆盖
