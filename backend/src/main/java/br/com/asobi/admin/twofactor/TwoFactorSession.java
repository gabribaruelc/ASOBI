package br.com.asobi.admin.twofactor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.asobi.admin.directory.AdminDirectory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Guarda na sessão do painel se o admin logado já digitou o código. O que fica gravado é o
 * e-mail conferido: se outra conta entrar na mesma sessão, o código é pedido de novo.
 */
@Component
public class TwoFactorSession {

	private static final String VERIFIED_EMAIL = "admin.twoFactor.verifiedEmail";
	private static final String CHALLENGE_ID = "admin.twoFactor.challengeId";

	// o código vale 10 minutos; depois disso a entrada não serve para mais nada
	private static final Duration FIRST_CODE_MEMORY = Duration.ofMinutes(10);

	private final TwoFactorGateway gateway;
	private final Map<String, FirstCode> firstCodes = new ConcurrentHashMap<>();

	public TwoFactorSession(TwoFactorGateway gateway) {
		this.gateway = gateway;
	}

	/** Com o segundo fator desligado, o login já basta. */
	public boolean isSatisfied(HttpServletRequest request, String email) {
		if (!gateway.isEnabled()) {
			return true;
		}
		HttpSession session = request.getSession(false);
		return session != null && email != null
				&& AdminDirectory.normalize(email).equals(session.getAttribute(VERIFIED_EMAIL));
	}

	public void markVerified(HttpSession session, String email) {
		session.setAttribute(VERIFIED_EMAIL, AdminDirectory.normalize(email));
		clearChallenge(session);
	}

	public String challengeId(HttpSession session) {
		return (String) session.getAttribute(CHALLENGE_ID);
	}

	public void setChallengeId(HttpSession session, String challengeId) {
		session.setAttribute(CHALLENGE_ID, challengeId);
	}

	public void clearChallenge(HttpSession session) {
		session.removeAttribute(CHALLENGE_ID);
		firstCodes.remove(session.getId());
	}

	/**
	 * Envia o código da primeira visita. A sessão fica no banco e só é gravada no fim da
	 * requisição, então duas requisições simultâneas (aba duplicada, pré-carregamento do
	 * navegador) veriam as duas "sem código" e sairiam dois e-mails, um deles já vencido.
	 * Aqui a segunda espera a primeira e fica com o mesmo desafio.
	 */
	public String requestFirstCode(HttpSession session, String email) {
		Instant now = Instant.now();
		firstCodes.values().removeIf(first -> first.requestedAt().isBefore(now.minus(FIRST_CODE_MEMORY)));
		String challengeId = firstCodes
				.computeIfAbsent(session.getId(), id -> new FirstCode(gateway.requestCode(email), now))
				.challengeId();
		setChallengeId(session, challengeId);
		return challengeId;
	}

	private record FirstCode(String challengeId, Instant requestedAt) {
	}
}
