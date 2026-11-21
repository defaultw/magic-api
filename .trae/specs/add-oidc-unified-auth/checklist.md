# Checklist

- [x] `Security.Oauth2` 含全部配置字段且默认值符合 spec（scopes=openid,profile、tokenValiditySeconds=28800、passwordLoginEnabled=true、usePkce=true、autoRedirect=false）
- [x] `OAuth2AuthorizationInterceptor`：token 过期惰性清理、createSession 随机 Magic-Token、logout 删除、refreshToken 滑动续期
- [x] 密码回退：passwordLoginEnabled=true 时委托 delegate 正常登录；false 时抛 MagicLoginException
- [x] `/oauth2/authorize`：state 一次性 + 5 分钟过期（callback 中 remove 后失效）、PKCE S256（usePkce=true 时）、302 至 IdP
- [x] `/oauth2/callback`：state 校验失败返回 401、code 换 token（form 表单）、userinfo 解析 sub/usernameClaim、签发内部 token 并 302 回 index.html（通过 query 参数 magic-token 传递，前端已同步支持 hash 与 query 两种方式）
- [x] issuer 发现模式：`{issuer}/.well-known/openid-configuration` 解析端点并缓存；显式 URI 优先
- [x] redirect-uri 未配置时从请求动态推导（绝对 URL，含非默认端口与 context-path 处理）
- [x] AutoConfiguration：oauth2.enabled 才装配新拦截器与控制器；未启用时行为与现状一致；用户自定义 AuthorizationInterceptor Bean 时自动装配跳过（instanceof 双保险）
- [x] config.json 下发 oauth2 节点（enabled/autoRedirect/authorizePath），无敏感信息
- [x] index.html：query/hash token 写入 localStorage `magic-token` 后清除参数；autoRedirect 自动跳转；登录框注入统一认证按钮；老用户 token 流程不受影响
- [x] WebSocket 工作台登录（getUserByToken 路径）对 OIDC 签发 token 校验通过（MagicWorkbenchHandler 经拦截器会话 Map 校验）
- [x] 未引入新第三方依赖；`mvn compile`（Java 8）通过（BUILD SUCCESS，exit 0）
- [x] 未开启 oauth2 时所有现有行为不变（回归无影响）
