package br.com.asobi.admin.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import br.com.asobi.admin.directory.AdminDirectory;

/** Só aceita e-mail verificado e liberado pelo admin-auth-service. */
@Component
public class AdminOidcUserService extends OidcUserService {

	private final AdminDirectory adminDirectory;

	public AdminOidcUserService(AdminDirectory adminDirectory) {
		this.adminDirectory = adminDirectory;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) {
		OidcUser user = super.loadUser(userRequest);
		boolean verified = Boolean.TRUE.equals(user.getEmailVerified());
		if (!verified || !adminDirectory.authorizeGoogleLogin(user.getIdToken().getTokenValue(), user.getEmail())) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("access_denied", "Este e-mail não tem acesso ao painel.", null));
		}
		return user;
	}
}
