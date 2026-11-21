# Tasks

- [x] Task 1: 扩展 Security 配置模型，新增 OAuth2 嵌套配置
  - [x] SubTask 1.1: 在 `magic-api/core/config/Security.java` 新增静态内部类 `Oauth2`，字段：enabled、issuer、clientId、clientSecret、scopes、authorizationUri、tokenUri、userinfoUri、redirectUri、usernameClaim、tokenValiditySeconds、autoRedirect、passwordLoginEnabled、usePkce 及对应 getter/setter
  - [x] SubTask 1.2: `Security` 增加 `oauth2` 属性与访问器；`Oauth2` 提供默认值（scopes=openid,profile、tokenValiditySeconds=28800、passwordLoginEnabled=true、usePkce=true、autoRedirect=false）
  - [x] SubTask 1.3: 在 `Security` 类添加 `isOauth2Enabled()` 便捷方法（enabled 且 issuer 或三个端点 URI 任一配置有效）

- [x] Task 2: 实现 OAuth2AuthorizationInterceptor（token 会话管理 + 密码回退）
  - [x] SubTask 2.1: 新建 `magic-api/core/interceptor/OAuth2AuthorizationInterceptor.java`，内部持有 `ConcurrentHashMap<String, MagicUser>` token 存储（条目含过期时间戳），`getUserByToken` 校验 token 存在且未过期（过期时移除并抛 MagicLoginException）
  - [x] SubTask 2.2: 实现 `createSession(String id, String username)` 生成随机 Magic-Token（SecureRandom 64 hex）并缓存 MagicUser（timeout=tokenValiditySeconds）
  - [x] SubTask 2.3: 实现 `logout(token)` 删除会话；`refreshToken(user)` 滑动续期（仅工作台心跳场景）
  - [x] SubTask 2.4: 构造函数接收 `AuthorizationInterceptor delegate`（默认密码实现），`requireLogin()` 恒 true；`login(username,password)` 在 `passwordLoginEnabled=true` 时委托 delegate，否则抛 MagicLoginException("密码登录已禁用，请使用统一认证登录")
  - [x] SubTask 2.5: `allowVisit` 系列方法委托 delegate，保持现有按钮级权限行为

- [x] Task 3: 实现 MagicOAuth2Controller（授权码流程两个端点）
  - [x] SubTask 3.1: 新建 `magic-api/core/web/MagicOAuth2Controller.java`，`@GetMapping("/oauth2/authorize")`：校验 oauth2 已启用（否则 404 提示未启用），生成 state（SecureRandom）与可选 PKCE（S256：verifier 64 字符随机串 → SHA-256 → Base64URL），state 存入带过期时间的内存 Map（5 分钟），构造授权 URL（response_type=code、client_id、redirect_uri、scope、state、nonce、code_challenge/code_challenge_method=S256）并 302（注：未加 `@Valid`——该注解仅对 MagicController 子类生效，本控制器独立于 @Valid 体系，天然免登录拦截；尾斜杠由 PathUtils 统一处理）
  - [x] SubTask 3.2: `@GetMapping("/oauth2/callback")`：从内存 Map 校验 state（无效/过期返回 401），取 code；RestTemplate POST token-uri（application/x-www-form-urlencoded：grant_type=authorization_code、code、redirect_uri、client_id、client_secret 或 PKCE code_verifier）；解析 access_token；GET userinfo-uri 携带 Bearer token，解析 sub 与 usernameClaim；调 `OAuth2AuthorizationInterceptor.createSession` 签发内部 token；302 重定向至 `{web}/index.html#magic-token=<token>`（state 一次性清除已用条目）
  - [x] SubTask 3.3: redirect-uri 推导逻辑：优先配置值；否则 `request.getScheme()+host+contextPath+web+"/oauth2/callback"`
  - [x] SubTask 3.4: 端点解析：优先显式 authorizationUri/tokenUri/userinfoUri；否则当配置 issuer 时，GET `{issuer}/.well-known/openid-configuration` 解析（结果缓存，失败抛出明确异常）；使用 `JsonUtils.readValue` 解析 JSON
  - [x] SubTask 3.5: 错误处理：IdP 错误回调（error 参数）返回 401 并提示；token 交换/userinfo 失败记录日志并返回 401

- [x] Task 4: MagicAPIAutoConfiguration 装配
  - [x] SubTask 4.1: oauth2.enabled 时 `authorizationInterceptor` Bean 返回 `OAuth2AuthorizationInterceptor`（内部包 `DefaultAuthorizationInterceptor`），否则保持原逻辑不变
  - [x] SubTask 4.2: oauth2.enabled 且开启 Web UI 时，通过 Mapping 注册 `MagicOAuth2Controller` 的 authorize/callback 两个端点（路径 = web 前缀 + /oauth2/...）
  - [x] SubTask 4.3: 确认 `@ConditionalOnMissingBean` 用户自定义 AuthorizationInterceptor 语义不被破坏（用户自定义 Bean 优先，此时 oauth2 自动装配跳过；注册控制器处额外以 instanceof 双保险）

- [x] Task 5: config.json 下发 OAuth2 前端配置
  - [x] SubTask 5.1: `MagicWorkbenchController.readConfig()` 增加 `oauth2` 节点：`{enabled: bool, autoRedirect: bool, authorizePath: "<web>/oauth2/authorize"}`（不泄露任何密钥）

- [x] Task 6: magic-editor/index.html 注入 OIDC 前端脚本
  - [x] SubTask 6.1: `magic-editor/src/main/resources/magic-editor/index.html` 新增内联 `<script>`：解析 `location.hash` 中 `magic-token=<token>` → 写 localStorage 键 `magic-token`（值直接为原始 token 字符串，与编辑器 De.get('token') 语义一致）→ `history.replaceState` 清除 hash
  - [x] SubTask 6.2: 脚本运行时 fetch `config.json`（相对路径）读取 oauth2 节点；若 `autoRedirect=true` 且 localStorage 无 `magic-token` 且当前 hash 无 token，则 `location.replace(authorizePath)`
  - [x] SubTask 6.3: 非 autoRedirect 场景：MutationObserver 监听登录框出现，向 `.magic-login-box` 卡片追加「统一认证登录」链接按钮（href=authorizePath，样式与现有按钮协调）；已存在则跳过，避免重复注入
  - [x] SubTask 6.4: 确保 token 已存在时（老密码登录用户）不影响现有自动登录流程

- [x] Task 7: 编译验证与文档说明
  - [x] SubTask 7.1: `mvn -pl magic-api,magic-api-spring-boot-starter -am compile` 通过（Java 8，BUILD SUCCESS，exit 0）
  - [x] SubTask 7.2: 确认 `magic-api` 与 `magic-api-spring-boot-starter` 未新增任何依赖（javax.servlet 由 spring-boot-starter-web 传递提供），无需改 pom

# Task Dependencies
- Task 2 依赖 Task 1（使用 Oauth2 配置）
- Task 3 依赖 Task 1、Task 2（使用配置与 createSession）
- Task 4 依赖 Task 2、Task 3
- Task 5、Task 6 依赖 Task 4 下发的配置结构（可与 Task 4 并行开发，联调依赖其完成）
- Task 7 依赖全部前置任务
