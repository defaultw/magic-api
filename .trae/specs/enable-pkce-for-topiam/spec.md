# 启用 PKCE 对齐 TOPIAM 强制要求 Spec

## Why
TOPIAM 授权端点返回 `[invalid_request] OAuth 2.0 Parameter: code_challenge`：客户端当前 `use-pkce: false` 未发送 code_challenge，而 TOPIAM（授权码模式）强制要求 PKCE。对照证据：19:41 携带 code_challenge 的请求仅报 invalid_scope（scope 问题已修复），从未报过 PKCE 问题——证实 PKCE 为必选，上一轮按截图判断的 use-pkce=false 需要反转。

## What Changes
- magic-api-demo `application.yml`：`use-pkce: false` → `true`，注释更新为「TOPIAM 强制要求 PKCE，必须开启」
- 顺带修正 L44 密码登录注释与实际值不符的陈述（用户已将 password-login-enabled 改为 false，原注释仍写"保留密码回退"）
- 无 Java 代码改动

## Impact
- Affected specs: fix-oidc-redirect-uri（use-pkce 取值反转）、add-oidc-unified-auth（usePkce 语义不变，仅取值对齐 IdP）
- Affected code: magic-api-demo/src/main/resources/application.yml（仅配置）

## ADDED Requirements
### Requirement: PKCE 必须启用
`use-pkce` SHALL 为 true：authorize 请求携带 `code_challenge`（S256）与 `code_challenge_method=S256`，token 交换携带 `code_verifier`，与 TOPIAM 强制 PKCE 要求一致。

#### Scenario: 授权请求通过 TOPIAM 校验
- **WHEN** 访问 `/magic/web/oauth2/authorize`
- **THEN** 302 Location 同时包含 redirect_uri（127.0.0.1 版本）、scope=openid、code_challenge 与 code_challenge_method=S256，TOPIAM 不再返回 invalid_request code_challenge

## MODIFIED Requirements
### Requirement: 密码登录注释准确性
application.yml 中 password-login-enabled 的注释 SHALL 与实际取值语义一致（当前 false = 仅 OIDC 登录，无密码回退）。

#### Scenario: 配置自解释
- **WHEN** 阅读 application.yml 安全配置段
- **THEN** 注释准确描述当前行为，不产生误导
