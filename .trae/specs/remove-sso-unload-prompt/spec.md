# 移除 SSO 跳转离开确认 Spec

## Why
点击「统一认证登录」跳转 TOPIAM 时浏览器弹出「离开当前网站？系统可能不会保存您所做的更改」确认框。根因：预编译 app.60f63c60.js 应用初始化时无条件执行 `l.blockClose !== false && (window.onbeforeunload = () => "系统可能不会保存您所做的更改。")`，登录页尚未做任何更改，该确认毫无意义且阻断 SSO 流程。

## What Changes
- index.html 注入脚本增强：SSO 按钮增加 click 监听，导航前同步 `window.onbeforeunload = null`
- autoRedirect 分支在 `location.replace(authorizeUrl)` 前同样解除（防 fetch 回调晚于 app.js 注册的竞态）
- 不改预编译 JS、不动 blockClose 全局配置——登录后编辑器的关闭保护（防误关丢失未保存内容）原样保留

## Impact
- Affected specs: add-oidc-unified-auth（index.html 注入脚本）、apple-style-login-page
- Affected code: magic-editor/src/main/resources/magic-editor/index.html（仅注入脚本）

## ADDED Requirements
### Requirement: SSO 跳转无离开确认
点击 SSO 按钮或 autoRedirect 自动跳转时，SHALL 同步移除 window.onbeforeunload 后再导航，浏览器不弹出离开确认。

#### Scenario: 点击统一认证登录
- **WHEN** 用户点击「统一认证登录 ›」按钮
- **THEN** 直接跳转 TOPIAM 授权页，无任何浏览器确认框

#### Scenario: 编辑器关闭保护不受影响
- **WHEN** 用户已登录并在编辑器中修改后关闭标签页
- **THEN** 仍弹出「系统可能不会保存您所做的更改」确认（blockClose 行为保留）

### Requirement: 竞态防护
autoRedirect 跳转 SHALL 在导航前解除 onbeforeunload，覆盖 fetch 回调先于/后于 app.js 模块执行的两种时序。
