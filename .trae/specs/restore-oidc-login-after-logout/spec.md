# 退出登录后恢复 OIDC 登录页 Spec

## Why
实测复现：编辑器内点退出（右下角 logout 图标 → 确定）后，SPA 在内部重建登录组件，页面不刷新，注入脚本的初始化逻辑（Apple 样式、body 类、MutationObserver 插入 SSO 按钮）不会重跑，导致用户看到原始密码登录页（无 SSO 按钮、无 Apple 样式），OIDC 流程断裂。

## What Changes
- index.html 注入脚本增加**轻量轮询守护**：每 800ms 检查一次登录态
- 触发条件：`localStorage.magic-token` 从存在变为不存在 且 `.magic-login` 遮罩可见 → `location.reload()` 整页刷新
- 整页刷新后注入脚本从头执行，走已被验证的首次加载路径（Apple 样式 + SSO 按钮 + autoRedirect 判定全部恢复）
- 不改预编译 JS，不动 app.js 的退出逻辑与 LOGOUT 事件链路

## Impact
- Affected specs: add-oidc-unified-auth（index.html 注入脚本）、apple-style-login-page
- Affected code: magic-editor/src/main/resources/magic-editor/index.html（仅注入脚本）

## ADDED Requirements
### Requirement: 退出后自动恢复 OIDC 登录页
用户在编辑器内退出登录后，系统 SHALL 在登录框出现时整页刷新，恢复 Apple 风格登录页与「统一认证登录」按钮。

#### Scenario: 编辑器内退出登录
- **WHEN** 用户点击右下角退出图标并确认注销
- **THEN** 约 1 秒内页面自动刷新，显示 Apple 风格登录页，SSO 按钮可见可用

#### Scenario: 正常登录不受影响
- **WHEN** 用户正常使用编辑器（已登录，token 存在）
- **THEN** 无任何刷新或干扰（守护逻辑只在 token 消失时动作）

### Requirement: 守护逻辑幂等轻量
- **WHEN** 页面处于登录页状态（无 token）
- **THEN** 守护检测到刷新完成后自动停止（刷新后无 token、无「已登录→登出」转变，不再次触发）
