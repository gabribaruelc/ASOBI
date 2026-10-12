package br.com.asobi.admin.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public final class CurrentAdmin {

	private CurrentAdmin() {
	}

	public static String emailOf(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getEmail();
		}
		return authentication.getName();
	}
}
