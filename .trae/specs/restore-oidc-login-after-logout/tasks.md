# Tasks
- [x] Task 1: index.html 注入脚本增加退出守护
  - [x] SubTask 1.1: 记录登录态：脚本内维护 `wasLoggedIn` 状态（token 存在时置 true）
  - [x] SubTask 1.2: 800ms 轮询：token 消失且 wasLoggedIn=true 且 `.magic-login` 可见 → `location.reload()`；刷新后 wasLoggedIn 重置，不会循环（实现补充：reload 前先 `window.onbeforeunload = null` 解除 app.js 注册的离开确认，避免刷新被「系统可能不会保存您所做的更改」原生弹窗拦截；守护置于 IIFE 提前 return 之前，覆盖 OIDC 回调进入的会话）
- [x] Task 2: 打包启动验证
  - [x] SubTask 2.1: `mvn -pl magic-api-demo -am clean package -DskipTests` 打包成功，后台启动 jar（cwd=magic-api-demo）
  - [x] SubTask 2.2: 浏览器实测完整闭环：登录 → 点退出 → 确定注销 → 页面自动刷新为 Apple 风格登录页（SSO 按钮可见）

# Task Dependencies
- Task 2 depends on Task 1

# 追加修复（用户反馈：退出后原始登录页短暂闪现）
- [x] Task 3: 消除退出后的原始登录页闪现
  - [x] SubTask 3.1: 样式/按钮注入同步化——appleCss 与 injectSso/applyLoginUi/initOauth2Ui 提升到 IIFE 顶层；首次用 sessionStorage 缓存的 oauth2 配置同步初始化（脚本解析期、Vue 挂载前即应用样式），fetch(config.json) 后用最新配置重新 initOauth2Ui 保证缓存一致
  - [x] SubTask 3.2: OIDC 回调路径不再提前 return——token 落库后继续走 UI 初始化（此时已有 token，autoRedirect 分支自然跳过），回调进入的会话也具备样式与退出守护
  - [x] SubTask 3.3: 退出守护降级为兜底——退出不再 reload：样式节点常驻、登录组件重建时 MutationObserver 自动重注入按钮（轮询间隔收紧至 300ms）；仅 UI 从未初始化（无缓存且 config.json 失败）时才整页刷新
  - [x] SubTask 3.4: 重打包 + 浏览器实测：SSO 登录 → 退出 → 确定注销 → 无 reload、无原始登录页闪现，Apple 登录页即时恢复（token=false、ssoBtn=true、appleStyleLen=4632、loginVisible=true），5 秒后状态稳定无循环，截图确认
