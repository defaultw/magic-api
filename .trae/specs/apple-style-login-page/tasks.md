# Tasks
- [x] Task 1: 后端暴露 passwordLoginEnabled
  - [x] SubTask 1.1: MagicWorkbenchController.readConfig() oauth2 节点增加 `passwordLoginEnabled`（getOauth2()==null 时兜底 true，保持旧行为兼容）
- [x] Task 2: index.html Apple 风格注入（已加载 frontend-design skill，用户明确指定 Apple 设计语言）
  - [x] SubTask 2.1: 反查预编译 CSS 确认精确选择器（.magic-login / .magic-login-box / .magic-login-logo / .magic-login-text / .magic-login-row / .magic-input / .magic-button / .magic-login-copyright，均带 data-v-cbcc40fa 作用域属性，覆盖用 !important）
  - [x] SubTask 2.2: 注入 Apple 风格 `<style id="magic-apple-login-style">`（系统字体栈/#f5f5f7 背景/白色 24px 圆角卡片+backdrop-blur+多层柔和阴影/输入框 12px 圆角+聚焦 #0071e3 光晕/pill 按钮 980px 圆角+hover-active 态/响应式+prefers-reduced-motion）
  - [x] SubTask 2.3: OIDC 按钮改为 Apple pill 主按钮（统一认证登录 ›，箭头 hover 位移微交互）；passwordLoginEnabled=false 时 body 加 magic-login-password-hidden 类隐藏账号密码表单
- [x] Task 3: 打包启动验证
  - [x] SubTask 3.1: `mvn -pl magic-api-demo -am clean package -DskipTests` exit 0，后台启动 jar（cwd=magic-api-demo）
  - [x] SubTask 3.2: config.json oauth2 节点含 passwordLoginEnabled:false；index.html(11180B) 含 magic-apple-login-style/pill 样式/隐藏逻辑
  - [x] SubTask 3.3: 浏览器实测 PASS（#f5f5f7 背景、24px 圆角卡片、蓝色 pill 按钮 312×52、零可见输入框、截图 magic-apple-login-verify.png）

# Task Dependencies
- Task 2 depends on Task 1；Task 3 depends on Task 1、2
