package br.com.asobi.adminauth.login;

import java.util.List;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Confere assinatura (chaves públicas do Google), validade, emissor e destinatário do ID token. */
public class NimbusGoogleTokenVerifier implements GoogleTokenVerifier {

	private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
	private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");

	private final JwtDecoder decoder;

	public NimbusGoogleTokenVerifier(String clientId) {
		NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
		nimbus.setJwtValidator(validator(clientId));
		this.decoder = nimbus;
	}

	static OAuth2TokenValidator<Jwt> validator(String clientId) {
		OAuth2TokenValidator<Jwt> issuer = new JwtClaimValidator<String>("iss", ISSUERS::contains);
		OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(
						new OAuth2Error("invalid_token", "Token emitido para outro aplicativo.", null));
		return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
	}

	@Override
	public GoogleIdentity verify(String idToken) {
		try {
			Jwt jwt = decoder.decode(idToken);
			return new GoogleIdentity(jwt.getClaimAsString("email"),
					Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
		} catch (JwtException ex) {
			throw new InvalidTokenException("Token do Google inválido.", ex);
		}
	}
}
