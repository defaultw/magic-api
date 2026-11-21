package org.ssssssss.magicapi.core.interceptor;

import org.ssssssss.magicapi.core.config.Security;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2/OIDC统一认证拦截器
 * <p>
 * 内部token会话存储（过期惰性清理），密码登录委托原实现作为回退
 *
 * @since 2.3.0
 */
public class OAuth2AuthorizationInterceptor implements AuthorizationInterceptor {

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	private final Security.Oauth2 oauth2Config;

	private final AuthorizationInterceptor delegate;

	/**
	 * 内部token会话存储
	 */
	private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();

	public OAuth2AuthorizationInterceptor(Security.Oauth2 oauth2Config, AuthorizationInterceptor delegate) {
		this.oauth2Config = oauth2Config;
		this.delegate = delegate;
	}

	public Security.Oauth2 getOauth2Config() {
		return oauth2Config;
	}

	@Override
	public boolean requireLogin() {
		return true;
	}

	@Override
	public MagicUser getUserByToken(String token) throws MagicLoginException {
		SessionEntry entry = getValidSession(token);
		if (entry == null) {
			throw new MagicLoginException("token无效或已过期");
		}
		return entry.user;
	}

	@Override
	public MagicUser login(String username, String password) throws MagicLoginException {
		if (!oauth2Config.isPasswordLoginEnabled()) {
			throw new MagicLoginException("密码登录已禁用，请使用统一认证登录");
		}
		MagicUser user = delegate.login(username, password);
		long validity = user.getTimeout() > 0 ? user.getTimeout() : oauth2Config.getTokenValiditySeconds();
		sessions.put(user.getToken(), new SessionEntry(user, System.currentTimeMillis() + validity * 1000));
		return user;
	}

	@Override
	public void logout(String token) {
		if (token != null) {
			sessions.remove(token);
		}
		delegate.logout(token);
	}

	@Override
	public void refreshToken(MagicUser user) {
		SessionEntry entry = user == null ? null : sessions.get(user.getToken());
		if (entry != null) {
			entry.expireAt = System.currentTimeMillis() + oauth2Config.getTokenValiditySeconds() * 1000;
			user.setTimeout(oauth2Config.getTokenValiditySeconds());
		}
		delegate.refreshToken(user);
	}

	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {
		return delegate.allowVisit(magicUser, request, authorization);
	}

	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization, MagicEntity entity) {
		return delegate.allowVisit(magicUser, request, authorization, entity);
	}

	@Override
	public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization, Group group) {
		return delegate.allowVisit(magicUser, request, authorization, group);
	}

	/**
	 * 创建OAuth2/OIDC登录会话，签发内部token
	 */
	public MagicUser createSession(String id, String username) {
		String token = randomHex(32);
		long validity = oauth2Config.getTokenValiditySeconds();
		MagicUser user = new MagicUser(id, username, token, validity);
		sessions.put(token, new SessionEntry(user, System.currentTimeMillis() + validity * 1000));
		return user;
	}

	private SessionEntry getValidSession(String token) {
		if (token == null) {
			return null;
		}
		SessionEntry entry = sessions.get(token);
		if (entry == null) {
			return null;
		}
		if (entry.expireAt < System.currentTimeMillis()) {
			// 过期惰性清理
			sessions.remove(token, entry);
			return null;
		}
		return entry;
	}

	/**
	 * 生成指定字节数的随机hex字符串
	 */
	private static String randomHex(int bytes) {
		SecureRandom random = new SecureRandom();
		byte[] buf = new byte[bytes];
		random.nextBytes(buf);
		char[] chars = new char[bytes * 2];
		for (int i = 0; i < buf.length; i++) {
			chars[i * 2] = HEX_CHARS[(buf[i] & 0xFF) >>> 4];
			chars[i * 2 + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}

	/**
	 * 会话条目
	 */
	static class SessionEntry {
		final MagicUser user;
		volatile long expireAt;

		SessionEntry(MagicUser user, long expireAt) {
			this.user = user;
			this.expireAt = expireAt;
		}
	}
}
