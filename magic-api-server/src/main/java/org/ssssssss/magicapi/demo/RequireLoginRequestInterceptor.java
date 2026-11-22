package org.ssssssss.magicapi.demo;

import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 接口登录鉴权拦截器（官方 RequestInterceptor 模式，见
 * https://www.ssssssss.org/magic-api/pages/security/api/#鉴权流程）
 * <p>
 * 接口（或其分组）在工作台配置「该接口需要登录才允许访问」选项
 * （Options.REQUIRE_LOGIN，require_login=true）后，校验请求头 Magic-Token：
 * 账号密码登录与 OIDC 登录统一签发 magic-token，均通过
 * AuthorizationInterceptor#getUserByToken 校验，天然兼容两种登录方式。
 * <p>
 * 未通过时返回 JsonBean(401) 中断请求；未开启选项的接口不受影响。
 * 通过 ObjectProvider 惰性获取 MagicConfiguration，避免与
 * magicConfiguration Bean 的创建产生循环依赖
 * （starter 在 magicConfiguration 创建过程中收集 RequestInterceptor Bean）。
 */
@Component
public class RequireLoginRequestInterceptor implements RequestInterceptor {

	private final ObjectProvider<MagicConfiguration> configurationProvider;

	public RequireLoginRequestInterceptor(ObjectProvider<MagicConfiguration> configurationProvider) {
		this.configurationProvider = configurationProvider;
	}

	@Override
	public Object preHandle(ApiInfo info, MagicScriptContext context, MagicHttpServletRequest request, MagicHttpServletResponse response) {
		// 未开启登录认证的接口（接口与分组均未配置 require_login）直接放行
		if (!"true".equals(info.getOptionValue(Options.REQUIRE_LOGIN))) {
			return null;
		}
		try {
			// 统一会话校验入口：账号密码与 OIDC 登录的 magic-token 均在此校验
			configurationProvider.getObject().getAuthorizationInterceptor()
					.getUserByToken(request.getHeader(Constants.MAGIC_TOKEN_HEADER));
			return null;
		} catch (MagicLoginException e) {
			// token 缺失、无效或已过期：中断请求，接口脚本不执行
			return new JsonBean<>(401, "用户未登录");
		}
	}
}
