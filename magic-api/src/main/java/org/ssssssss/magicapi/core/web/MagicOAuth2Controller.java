package org.ssssssss.magicapi.core.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.Security;
import org.ssssssss.magicapi.core.exception.MagicAPIException;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.interceptor.OAuth2AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.utils.JsonUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2/OIDC统一认证登录控制器
 * <p>
 * 提供{@code /oauth2/authorize}与{@code /oauth2/callback}两个端点，
 * 实现标准授权码流程（含PKCE），登录成功后签发内部Magic-Token并重定向回编辑器
 *
 * @since 2.3.0
 */
@Controller
public class MagicOAuth2Controller {

	private static final Logger logger = LoggerFactory.getLogger(MagicOAuth2Controller.class);

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	private static final String PKCE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

	/**
	 * state有效期：5分钟
	 */
	private static final long STATE_EXPIRE_MILLIS = 5 * 60 * 1000L;

	private final MagicAPIProperties properties;

	private final OAuth2AuthorizationInterceptor oauth2Interceptor;

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * 一次性state存储
	 */
	private final ConcurrentHashMap<String, StateEntry> states = new ConcurrentHashMap<>();

	/**
	 * OIDC发现结果缓存
	 */
	private volatile Endpoints cachedEndpoints;

	public MagicOAuth2Controller(MagicAPIProperties properties, OAuth2AuthorizationInterceptor oauth2Interceptor) {
		this.properties = properties;
		this.oauth2Interceptor = oauth2Interceptor;
	}

	/**
	 * 跳转IdP授权端点
	 */
	@GetMapping("/oauth2/authorize")
	public String authorize(HttpServletRequest request) {
		Security security = properties.getSecurity();
		if (!security.isOauth2Enabled()) {
			throw new MagicAPIException("OAuth2登录未启用");
		}
		Security.Oauth2 config = oauth2Interceptor.getOauth2Config();
		// 清理过期的state
		long now = System.currentTimeMillis();
		for (Map.Entry<String, StateEntry> entry : states.entrySet()) {
			if (entry.getValue().expireAt < now) {
				states.remove(entry.getKey());
			}
		}
		String state = randomHex(16);
		String verifier = null;
		String authorizationUrl;
		try {
			StringBuilder builder = new StringBuilder(resolveEndpoints().authorizationUri);
			String redirectUri = resolveRedirectUri(request);
			builder.append(builder.toString().contains("?") ? '&' : '?')
					.append("response_type=code")
					.append("&client_id=").append(urlEncode(config.getClientId()))
					.append("&redirect_uri=").append(urlEncode(redirectUri))
					.append("&scope=").append(urlEncode(config.getScopes() == null ? "openid" : config.getScopes().replace(',', ' ')))
					.append("&state=").append(state)
					.append("&nonce=").append(state);
			if (config.isUsePkce()) {
				verifier = randomVerifier();
				builder.append("&code_challenge=").append(base64Url(sha256(verifier)))
						.append("&code_challenge_method=S256");
			}
			authorizationUrl = builder.toString();
			states.put(state, new StateEntry(verifier, now + STATE_EXPIRE_MILLIS, redirectUri));
		} catch (MagicAPIException e) {
			throw e;
		} catch (Exception e) {
			logger.error("构造OAuth2授权地址失败", e);
			throw new MagicAPIException("构造OAuth2授权地址失败", e);
		}
		logger.info("跳转OAuth2授权端点: {}", authorizationUrl);
		return "redirect:" + authorizationUrl;
	}

	/**
	 * IdP回调：用code换取token，获取用户信息并签发内部token
	 */
	@GetMapping("/oauth2/callback")
	public String callback(HttpServletRequest request,
						   @RequestParam(value = "code", required = false) String code,
						   @RequestParam(value = "state", required = false) String state,
						   @RequestParam(value = "error", required = false) String error) throws MagicLoginException {
		if (error != null && !error.isEmpty()) {
			logger.warn("OAuth2授权失败, error: {}, description: {}", error, request.getParameter("error_description"));
			throw new MagicLoginException("授权失败: " + error);
		}
		StateEntry stateEntry = state == null ? null : states.remove(state);
		if (stateEntry == null || stateEntry.expireAt < System.currentTimeMillis()) {
			logger.warn("OAuth2回调state无效或已过期");
			throw new MagicLoginException("state无效或已过期");
		}
		if (code == null || code.isEmpty()) {
			logger.warn("OAuth2回调缺少code参数");
			throw new MagicLoginException("授权码code为空");
		}
		Security.Oauth2 config = oauth2Interceptor.getOauth2Config();
		Endpoints endpoints = resolveEndpoints();
		String accessToken;
		try {
			accessToken = exchangeToken(endpoints.tokenUri, config, code, stateEntry.redirectUri, stateEntry.verifier);
		} catch (MagicLoginException e) {
			throw e;
		} catch (Exception e) {
			logger.error("OAuth2换取token失败, tokenUri: {}", endpoints.tokenUri, e);
			throw new MagicLoginException("换取token失败");
		}
		String sub;
		String username;
		try {
			Map<String, Object> userinfo = requestUserinfo(endpoints.userinfoUri, accessToken);
			sub = userinfo.get("sub") == null ? null : String.valueOf(userinfo.get("sub"));
			String claim = config.getUsernameClaim() == null ? "preferred_username" : config.getUsernameClaim();
			Object usernameValue = userinfo.get(claim);
			username = usernameValue == null ? sub : String.valueOf(usernameValue);
		} catch (MagicLoginException e) {
			throw e;
		} catch (Exception e) {
			logger.error("OAuth2获取用户信息失败, userinfoUri: {}", endpoints.userinfoUri, e);
			throw new MagicLoginException("获取用户信息失败");
		}
		if (sub == null || sub.isEmpty()) {
			logger.warn("OAuth2用户信息缺少sub字段, userinfoUri: {}", endpoints.userinfoUri);
			throw new MagicLoginException("用户信息缺少sub字段");
		}
		String token = oauth2Interceptor.createSession(sub, username).getToken();
		// 基于{contextPath}{web}根路径重定向到编辑器，通过query传递token（Location中比hash更可靠）
		String target = resolveWebBase(request) + "/index.html?magic-token=" + urlEncode(token);
		return "redirect:" + target;
	}

	@ExceptionHandler(MagicLoginException.class)
	@ResponseBody
	public JsonBean<Void> invalidLogin(MagicLoginException exception) {
		return new JsonBean<>(401, exception.getMessage());
	}

	@ExceptionHandler(MagicAPIException.class)
	@ResponseBody
	public JsonBean<Void> invalidRequest(MagicAPIException exception) {
		return new JsonBean<>(404, exception.getMessage());
	}

	/**
	 * 解析授权端点/token端点/userinfo端点（显式配置优先，否则走OIDC发现，结果缓存）
	 */
	private Endpoints resolveEndpoints() {
		Endpoints endpoints = cachedEndpoints;
		if (endpoints != null) {
			return endpoints;
		}
		Security.Oauth2 config = oauth2Interceptor.getOauth2Config();
		String authorizationUri = config.getAuthorizationUri();
		String tokenUri = config.getTokenUri();
		String userinfoUri = config.getUserinfoUri();
		if (isBlank(authorizationUri) || isBlank(tokenUri) || isBlank(userinfoUri)) {
			if (isBlank(config.getIssuer())) {
				throw new MagicAPIException("OIDC端点未配置，请配置issuer或authorization-uri/token-uri/userinfo-uri");
			}
			String discoveryUrl = config.getIssuer();
			if (!discoveryUrl.endsWith("/")) {
				discoveryUrl += "/";
			}
			discoveryUrl += ".well-known/openid-configuration";
			Map<String, Object> discovery;
			try {
				discovery = JsonUtils.readValue(restTemplate.getForEntity(discoveryUrl, String.class).getBody(), Map.class);
			} catch (Exception e) {
				logger.error("获取OIDC发现配置失败, url: {}", discoveryUrl, e);
				throw new MagicAPIException("无法获取OIDC发现配置: " + discoveryUrl, e);
			}
			if (discovery == null) {
				logger.error("OIDC发现配置解析失败, url: {}", discoveryUrl);
				throw new MagicAPIException("无法获取OIDC发现配置: " + discoveryUrl);
			}
			if (isBlank(authorizationUri)) {
				authorizationUri = asString(discovery.get("authorization_endpoint"));
			}
			if (isBlank(tokenUri)) {
				tokenUri = asString(discovery.get("token_endpoint"));
			}
			if (isBlank(userinfoUri)) {
				userinfoUri = asString(discovery.get("userinfo_endpoint"));
			}
		}
		if (isBlank(authorizationUri) || isBlank(tokenUri) || isBlank(userinfoUri)) {
			throw new MagicAPIException("OIDC端点未配置，请配置issuer或authorization-uri/token-uri/userinfo-uri");
		}
		endpoints = new Endpoints(authorizationUri, tokenUri, userinfoUri);
		cachedEndpoints = endpoints;
		return endpoints;
	}

	/**
	 * 用授权码换取access_token
	 */
	private String exchangeToken(String tokenUri, Security.Oauth2 config, String code, String redirectUri, String verifier) throws MagicLoginException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("code", code);
		body.add("redirect_uri", redirectUri);
		if (!isBlank(config.getClientSecret())) {
			body.add("client_id", config.getClientId());
			body.add("client_secret", config.getClientSecret());
		}
		if (config.isUsePkce() && verifier != null) {
			body.add("code_verifier", verifier);
		}
		String response = restTemplate.postForEntity(tokenUri, new HttpEntity<>(body, headers), String.class).getBody();
		Map<String, Object> tokenMap = JsonUtils.readValue(response, Map.class);
		if (tokenMap == null || tokenMap.get("access_token") == null) {
			logger.warn("OAuth2响应中缺少access_token, tokenUri: {}, response: {}", tokenUri, response);
			throw new MagicLoginException("获取access_token失败");
		}
		return String.valueOf(tokenMap.get("access_token"));
	}

	/**
	 * 调用userinfo端点获取用户信息
	 */
	private Map<String, Object> requestUserinfo(String userinfoUri, String accessToken) throws MagicLoginException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		String response = restTemplate.exchange(userinfoUri, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
		Map<String, Object> userinfo = JsonUtils.readValue(response, Map.class);
		if (userinfo == null) {
			logger.warn("OAuth2用户信息解析失败, userinfoUri: {}, response: {}", userinfoUri, response);
			throw new MagicLoginException("获取用户信息失败");
		}
		return userinfo;
	}

	/**
	 * 推导回调地址：优先配置值，否则从当前请求推导（OAuth2协议要求redirect_uri为绝对地址）
	 */
	private String resolveRedirectUri(HttpServletRequest request) {
		String redirectUri = oauth2Interceptor.getOauth2Config().getRedirectUri();
		if (!isBlank(redirectUri)) {
			return redirectUri;
		}
		String scheme = request.getScheme();
		int port = request.getServerPort();
		StringBuilder builder = new StringBuilder();
		builder.append(scheme).append("://").append(request.getServerName());
		if (port > 0 && !("http".equals(scheme) && port == 80) && !("https".equals(scheme) && port == 443)) {
			builder.append(':').append(port);
		}
		builder.append(resolveWebBase(request)).append("/oauth2/callback");
		// 注意：此处不能调用PathUtils.replaceSlash，其"/+"正则会把协议头的"://"压成":/"
		return builder.toString();
	}

	/**
	 * 推导web根路径（{contextPath}{web}），从当前请求URI截取
	 */
	private String resolveWebBase(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String suffix = "/oauth2/authorize";
		String suffixCallback = "/oauth2/callback";
		if (requestUri.endsWith(suffixCallback)) {
			return requestUri.substring(0, requestUri.length() - suffixCallback.length());
		}
		if (requestUri.endsWith(suffix)) {
			return requestUri.substring(0, requestUri.length() - suffix.length());
		}
		return request.getContextPath();
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static String urlEncode(String value) {
		try {
			return java.net.URLEncoder.encode(value == null ? "" : value, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			// UTF-8 在所有 JVM 实现中必支持，此处仅作兜底
			throw new IllegalStateException("UTF-8 not supported", e);
		}
	}

	/**
	 * 生成32位hex随机串
	 */
	private static String randomHex(int bytes) {
		byte[] buf = new byte[bytes];
		RANDOM.nextBytes(buf);
		char[] chars = new char[bytes * 2];
		for (int i = 0; i < buf.length; i++) {
			chars[i * 2] = HEX_CHARS[(buf[i] & 0xFF) >>> 4];
			chars[i * 2 + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	/**
	 * 生成64字符PKCE verifier
	 */
	private static String randomVerifier() {
		StringBuilder builder = new StringBuilder(64);
		for (int i = 0; i < 64; i++) {
			builder.append(PKCE_CHARS.charAt(RANDOM.nextInt(PKCE_CHARS.length())));
		}
		return builder.toString();
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String base64Url(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * 一次性state条目
	 */
	private static class StateEntry {
		final String verifier;
		final long expireAt;
		final String redirectUri;

		StateEntry(String verifier, long expireAt, String redirectUri) {
			this.verifier = verifier;
			this.expireAt = expireAt;
			this.redirectUri = redirectUri;
		}
	}

	/**
	 * OIDC端点集合
	 */
	private static class Endpoints {
		final String authorizationUri;
		final String tokenUri;
		final String userinfoUri;

		Endpoints(String authorizationUri, String tokenUri, String userinfoUri) {
			this.authorizationUri = authorizationUri;
			this.tokenUri = tokenUri;
			this.userinfoUri = userinfoUri;
		}
	}
}
