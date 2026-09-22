package br.com.asobi.admin.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import br.com.asobi.admin.service.AdminUserService;

/**
 * Depois do "Entrar com Google", só deixa entrar quem tem e-mail verificado e
 * cadastrado em admin_users. Os demais voltam para a tela de login com erro.
 */
@Component
public class AdminOidcUserService extends OidcUserService {

	private final AdminUserService adminUserService;

	public AdminOidcUserService(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) {
		OidcUser user = super.loadUser(userRequest);
		boolean verified = Boolean.TRUE.equals(user.getEmailVerified());
		if (!verified || !adminUserService.isAdmin(user.getEmail())) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("access_denied", "Este e-mail não tem acesso ao painel.", null));
		}
		return user;
	}
}
