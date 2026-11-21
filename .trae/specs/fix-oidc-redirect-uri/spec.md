# 修复 OIDC redirect_uri 白名单不匹配 Spec

## Why
实测 TOPIAM 授权端点返回 `[invalid_request] OAuth 2.0 Parameter: redirect_uri`：客户端下发的 redirect_uri 为 `http://localhost:9999/magic/web/oauth2/callback`（从请求 Host 推导），而 TOPIAM 应用「登录 Redirect URI 白名单」配置的是 `http://127.0.1:9999/magic/web/oauth2/callback`（且存在 `127.0.1` 笔误，少一个 `.0`）。OAuth2 白名单为逐字符串精确匹配，两者不一致即被 IdP 拒绝。同时截图中 TOPIAM 的 PKCE 开关为「关」，而客户端 `use-pkce` 默认 true 会发送 code_challenge，白名单修复后将触发下一个 PKCE 类报错，需一并对齐。

## What Changes
- magic-api-demo `application.yml`：显式配置 `security.oauth2.redirect-uri`，不再依赖从浏览器地址推导，保证与 TOPIAM 白名单完全一致
- magic-api-demo `application.yml`：显式配置 `use-pkce` 并注释说明必须与 TOPIAM「PKCE 开关」保持一致
- （用户操作，非代码）TOPIAM 控制台：登录 Redirect URI 白名单修正/添加 `http://localhost:9999/magic/web/oauth2/callback`；PKCE 开关建议开启（与 use-pkce: true 一致）
- 无 Java 代码改动

## Impact
- Affected specs: add-oidc-unified-auth（redirectUri/usePkce 配置项的使用说明）、add-oidc-demo-app（demo 配置）
- Affected code: magic-api-demo/src/main/resources/application.yml（仅配置）

## ADDED Requirements
### Requirement: redirect_uri 与白名单确定性一致
demo 应用的 OIDC redirect_uri SHALL 通过 `magic-api.security.oauth2.redirect-uri` 显式指定，且值必须与 TOPIAM 应用「登录 Redirect URI 白名单」中的某一条完全一致（协议、主机、端口、路径逐字符匹配）。

#### Scenario: 授权跳转携带正确 redirect_uri
- **WHEN** 访问 `http://localhost:9999/magic/web/oauth2/authorize`
- **THEN** 302 Location 中 `redirect_uri` 解码后等于 `http://localhost:9999/magic/web/oauth2/callback`

#### Scenario: TOPIAM 接受授权请求
- **WHEN** 用户在编辑器点击「统一认证登录 / OIDC Login」
- **THEN** TOPIAM 不再返回 invalid_request redirect_uri，正常进入登录/授权页

### Requirement: PKCE 行为与 IdP 开关对齐
`use-pkce` SHALL 显式配置并注明：其值必须与 TOPIAM 应用的 PKCE 开关一致（TOPIAM 开启 → true，授权请求携带 code_challenge/code_challenge_method=S256 且 token 请求携带 code_verifier；TOPIAM 关闭 → false，不携带任何 PKCE 参数）。

#### Scenario: PKCE 开关一致
- **WHEN** TOPIAM 应用开启 PKCE 且 use-pkce: true
- **THEN** 授权与换 token 全流程成功，无 PKCE 相关报错
