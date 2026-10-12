package br.com.asobi.adminauth.login;

public interface GoogleTokenVerifier {

	/**
	 * @throws InvalidTokenException     token adulterado, vencido ou emitido para outro aplicativo
	 * @throws LoginUnavailableException sem GOOGLE_CLIENT_ID configurado
	 */
	GoogleIdentity verify(String idToken);

	record GoogleIdentity(String email, boolean emailVerified) {
	}
}
