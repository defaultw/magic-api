# Checklist

- [x] RequireLoginRequestInterceptor 实现官方 RequestInterceptor 模式（preHandle 返回 JsonBean(401) 中断，返回 null 放行）
- [x] 仅当接口/分组配置 require_login=true 时才校验，其余接口行为不变
- [x] 校验入口统一走 configuration.getAuthorizationInterceptor().getUserByToken(Magic-Token)，账号密码与 OIDC 会话均可通过
- [x] 编译打包成功，jar 正常启动
- [x] 无 token 访问开启选项的接口返回 401「用户未登录」
- [x] 伪造/过期 token 访问返回 401
- [x] 账号密码登录后携带 Magic-Token 访问返回正常结果
- [x] 未开启登录认证的接口匿名访问行为与现状一致
