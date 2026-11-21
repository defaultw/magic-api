# Tasks
- [ ] Task 1: index.html 注入脚本增加 onbeforeunload 解除
  - [ ] SubTask 1.1: SSO 按钮创建时绑定 click 监听：`window.onbeforeunload = null`（导航由默认 href 行为继续）
  - [ ] SubTask 1.2: autoRedirect 分支 `location.replace` 前同步解除
- [ ] Task 2: 打包启动验证
  - [ ] SubTask 2.1: `mvn -pl magic-api-demo -am clean package -DskipTests` 打包成功，后台启动 jar（cwd=magic-api-demo）
  - [ ] SubTask 2.2: 浏览器实测：点击「统一认证登录 ›」直接跳 TOPIAM 无确认框；登录页 DOM 中按钮监听生效

# Task Dependencies
- Task 2 depends on Task 1
