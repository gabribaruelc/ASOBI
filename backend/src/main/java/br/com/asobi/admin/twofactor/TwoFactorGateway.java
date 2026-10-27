package br.com.asobi.admin.twofactor;

/**
 * Segundo fator do painel: um código enviado para o e-mail do admin. Quem gera, envia e
 * confere é o two-factor-service; o backend só pede e pergunta.
 */
public interface TwoFactorGateway {

	/** Sem o serviço configurado, o painel pede só o login. */
	boolean isEnabled();

	/**
	 * Envia um código novo para o e-mail.
	 *
	 * @return id do pedido de código (challenge), para conferir o código depois
	 * @throws br.com.asobi.common.exception.BusinessException com mensagem pronta para a tela
	 */
	String requestCode(String email);

	/** Nunca libera na dúvida: serviço fora do ar ou resposta estranha não é {@code VERIFIED}. */
	Verification verify(String challengeId, String code, String email);

	/** @param message texto pronto para a tela (null quando o código confere) */
	record Verification(Status status, String message) {

		public enum Status {
			VERIFIED,
			/** Errou, mas ainda pode tentar o mesmo código. */
			WRONG_CODE,
			/** Vencido, já usado ou bloqueado: é preciso pedir outro. */
			EXPIRED,
			UNAVAILABLE
		}
	}
}
