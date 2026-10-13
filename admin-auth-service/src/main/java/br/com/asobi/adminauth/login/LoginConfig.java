package br.com.asobi.adminauth.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import br.com.asobi.adminauth.config.AdminAuthProperties;

@Configuration
public class LoginConfig {

	@Bean
	public GoogleTokenVerifier googleTokenVerifier(AdminAuthProperties properties) {
		String clientId = properties.google() == null ? null : properties.google().clientId();
		if (StringUtils.hasText(clientId)) {
			return new NimbusGoogleTokenVerifier(clientId);
		}
		return idToken -> {
			throw new LoginUnavailableException("Login com Google não configurado (GOOGLE_CLIENT_ID).");
		};
	}
}
