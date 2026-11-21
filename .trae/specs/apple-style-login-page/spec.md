# Apple 风格登录页优化 Spec

## Why
当前编辑器登录页视觉简陋：预编译 Vue 产物自带的账号密码表单样式陈旧，且 demo 配置 `password-login-enabled: false` 时该表单为死 UI，再加上注入的 OIDC 按钮是朴素链接样式。用户要求按 Apple 设计语言优化。前端为预编译产物，唯一可编辑入口是 `magic-editor/src/main/resources/magic-editor/index.html`（沿用 add-oidc-unified-auth 的注入方案，不可改预编译资源）。

## What Changes
- index.html 注入 Apple 风格 `<style>`：系统字体栈（-apple-system/SF Pro/PingFang SC）、#f5f5f7 背景、白色圆角卡片（~18px）+ 柔和阴影、输入框圆角 + 聚焦蓝色光晕、Apple 风格主按钮（pill 圆角、#0071e3、hover/active/focus 态）
- 注入的 OIDC 按钮重新设计为 Apple pill 主按钮（替换现有朴素链接样式）
- MagicWorkbenchController.readConfig() 的 oauth2 节点新增 `passwordLoginEnabled` 字段（仅布尔值，无敏感信息）
- 注入脚本增强：oauth2.enabled 且 passwordLoginEnabled=false 时隐藏登录框内无效的账号密码表单，仅保留标题 + SSO 按钮 + 版权；true 时保留表单并同样美化
- 实现时使用 frontend-design skill 的设计规范
- 不改动预编译前端资源（js/css），CSS 通过选择器覆盖

## Impact
- Affected specs: add-oidc-unified-auth（index.html 注入脚本与 readConfig 扩展）、add-oidc-demo-app
- Affected code: magic-editor/src/main/resources/magic-editor/index.html；magic-api/src/main/java/org/ssssssss/magicapi/core/web/MagicWorkbenchController.java（readConfig 增加一个字段）

## ADDED Requirements
### Requirement: Apple 风格登录页视觉
登录页 SHALL 呈现 Apple 设计语言：系统字体栈、#f5f5f7 底色、白色大圆角卡片与柔和阴影、圆角输入框（聚焦蓝色光晕）、pill 主按钮；OIDC 按钮为页面主操作，视觉层级最高。

#### Scenario: 打开登录页
- **WHEN** 未登录访问 `/magic/web/index.html`
- **THEN** 登录卡片为 Apple 风格，OIDC 按钮为 pill 主按钮，整体无陈旧样式残留（注入样式优先级覆盖预编译 CSS）

#### Scenario: 密码登录禁用
- **WHEN** `password-login-enabled: false` 且 oauth2.enabled=true
- **THEN** 登录框隐藏账号密码输入与原登录按钮，仅显示标题 + 「统一认证登录」pill 按钮 + 版权

#### Scenario: 密码登录启用
- **WHEN** `password-login-enabled: true`
- **THEN** 账号密码表单保留可见，且同样应用 Apple 风格（可回退登录）

### Requirement: 幂等注入与兼容性
样式与脚本注入 SHALL 幂等（重复执行不产生重复节点）；oauth2.enabled=false 时 SHALL 不注入任何样式/按钮，登录页保持原样。

#### Scenario: 未开启 OIDC
- **WHEN** oauth2.enabled=false
- **THEN** index.html 行为与改造前完全一致
