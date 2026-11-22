package org.ssssssss.magicapi.mcp;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * MCP 模块自动装配（零配置可用）
 *
 * @since 2.3.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "magic-api.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration implements WebMvcConfigurer {

	private final String token;

	private final String mcpPath;

	public McpAutoConfiguration(@Value("${magic-api.mcp.token:}") String token,
								@Value("${magic-api.mcp.path:/mcp/magic}") String mcpPath) {
		this.token = token;
		this.mcpPath = mcpPath;
	}

	@Bean
	@ConditionalOnMissingBean(MagicApiTools.class)
	public MagicApiTools magicApiTools(MagicConfiguration configuration,
									   @Autowired(required = false) List<Plugin> plugins) {
		return new MagicApiTools(configuration, plugins);
	}

	@Bean
	@ConditionalOnMissingBean(McpController.class)
	public McpController mcpController(MagicApiTools tools,
									   @Value("${magic-api.mcp.server-name:magic-api-mcp}") String serverName) {
		return new McpController(tools, serverName);
	}

	/**
	 * 可选鉴权拦截器：magic-api.mcp.token 非空时校验 X-Mcp-Token / Authorization: Bearer
	 */
	@Bean
	@ConditionalOnMissingBean(name = "magicApiMcpTokenInterceptor")
	public HandlerInterceptor magicApiMcpTokenInterceptor() {
		if (StringUtils.isBlank(token)) {
			return new HandlerInterceptor() {
				@Override
				public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
					return true;
				}
			};
		}
		return new HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				String provided = request.getHeader("X-Mcp-Token");
				if (StringUtils.isBlank(provided)) {
					String authorization = request.getHeader("Authorization");
					if (authorization != null && authorization.startsWith("Bearer ")) {
						provided = authorization.substring(7);
					}
				}
				if (!token.equals(provided)) {
					response.setStatus(401);
					response.setContentType("application/json;charset=UTF-8");
					response.getWriter().write("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32001,\"message\":\"未授权: 缺少或错误的 MCP Token\"}}");
					return false;
				}
				return true;
			}
		};
	}

	/**
	 * 将 token 拦截器注册到 MVC 拦截器链（仅拦截 MCP 端点路径）
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(magicApiMcpTokenInterceptor()).addPathPatterns(mcpPath);
	}
}
