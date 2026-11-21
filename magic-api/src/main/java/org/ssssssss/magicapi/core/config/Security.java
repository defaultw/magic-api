package org.ssssssss.magicapi.core.config;

import org.apache.commons.lang3.StringUtils;

/**
 * 安全配置
 *
 * @author mxd
 * @since 0.4.0
 */
public class Security {

	/**
	 * 登录用的用户名
	 */
	private String username;

	/**
	 * 登录用的密码
	 */
	private String password;

	/**
	 * OAuth2/OIDC统一认证配置
	 *
	 * @since 2.3.0
	 */
	private Oauth2 oauth2 = new Oauth2();

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Oauth2 getOauth2() {
		return oauth2;
	}

	public void setOauth2(Oauth2 oauth2) {
		this.oauth2 = oauth2;
	}

	/**
	 * OAuth2/OIDC统一认证是否可用
	 *
	 * @since 2.3.0
	 */
	public boolean isOauth2Enabled() {
		if (oauth2 == null || !oauth2.isEnabled()) {
			return false;
		}
		return StringUtils.isNotBlank(oauth2.getIssuer())
				|| StringUtils.isNotBlank(oauth2.getAuthorizationUri())
				|| StringUtils.isNotBlank(oauth2.getTokenUri())
				|| StringUtils.isNotBlank(oauth2.getUserinfoUri());
	}

	/**
	 * OAuth2/OIDC统一认证配置
	 *
	 * @since 2.3.0
	 */
	public static class Oauth2 {

		/**
		 * 是否启用OAuth2/OIDC统一认证
		 */
		private boolean enabled = false;

		/**
		 * OIDC Issuer，如 https://xxx/auth/realms/xxx，配置后自动发现端点
		 */
		private String issuer;

		/**
		 * 客户端ID
		 */
		private String clientId;

		/**
		 * 客户端密钥
		 */
		private String clientSecret;

		/**
		 * 申请的scope，多个用,分隔
		 */
		private String scopes = "openid,profile";

		/**
		 * 授权端点地址（显式覆盖自动发现）
		 */
		private String authorizationUri;

		/**
		 * token端点地址（显式覆盖自动发现）
		 */
		private String tokenUri;

		/**
		 * userinfo端点地址（显式覆盖自动发现）
		 */
		private String userinfoUri;

		/**
		 * 回调地址（可选，不配置时从当前请求动态推导）
		 */
		private String redirectUri;

		/**
		 * 用户名字段，默认preferred_username，取不到时回退sub
		 */
		private String usernameClaim = "preferred_username";

		/**
		 * 内部token有效期（秒），默认8小时
		 */
		private long tokenValiditySeconds = 28800;

		/**
		 * 未登录时是否自动跳转统一认证
		 */
		private boolean autoRedirect = false;

		/**
		 * 是否保留账号密码登录
		 */
		private boolean passwordLoginEnabled = true;

		/**
		 * 是否使用PKCE（S256）
		 */
		private boolean usePkce = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getIssuer() {
			return issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getScopes() {
			return scopes;
		}

		public void setScopes(String scopes) {
			this.scopes = scopes;
		}

		public String getAuthorizationUri() {
			return authorizationUri;
		}

		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		public String getTokenUri() {
			return tokenUri;
		}

		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		public String getUserinfoUri() {
			return userinfoUri;
		}

		public void setUserinfoUri(String userinfoUri) {
			this.userinfoUri = userinfoUri;
		}

		public String getRedirectUri() {
			return redirectUri;
		}

		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}

		public String getUsernameClaim() {
			return usernameClaim;
		}

		public void setUsernameClaim(String usernameClaim) {
			this.usernameClaim = usernameClaim;
		}

		public long getTokenValiditySeconds() {
			return tokenValiditySeconds;
		}

		public void setTokenValiditySeconds(long tokenValiditySeconds) {
			this.tokenValiditySeconds = tokenValiditySeconds;
		}

		public boolean isAutoRedirect() {
			return autoRedirect;
		}

		public void setAutoRedirect(boolean autoRedirect) {
			this.autoRedirect = autoRedirect;
		}

		public boolean isPasswordLoginEnabled() {
			return passwordLoginEnabled;
		}

		public void setPasswordLoginEnabled(boolean passwordLoginEnabled) {
			this.passwordLoginEnabled = passwordLoginEnabled;
		}

		public boolean isUsePkce() {
			return usePkce;
		}

		public void setUsePkce(boolean usePkce) {
			this.usePkce = usePkce;
		}
	}
}
