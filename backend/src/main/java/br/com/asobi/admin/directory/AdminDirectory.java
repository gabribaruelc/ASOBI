package br.com.asobi.admin.directory;

import java.util.List;
import java.util.Locale;

/**
 * Quem pode entrar no painel. A lista mora no admin-auth-service; o backend só pergunta.
 * Mensagens das {@code BusinessException} já vêm prontas para mostrar no painel.
 */
public interface AdminDirectory {

	boolean isAdmin(String email);

	/**
	 * Login com Google: o serviço confere o ID token e se o e-mail dele está na lista.
	 *
	 * @param email e-mail que o backend leu do mesmo token
	 */
	boolean authorizeGoogleLogin(String idToken, String email);

	List<AdminAccount> listAdmins();

	AdminAccount addAdmin(String email, String createdByEmail);

	void removeAdmin(Long id, String currentEmail);

	static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
