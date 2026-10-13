package br.com.asobi.notification.email;

import java.util.List;

/** Quem de fato entrega os e-mails (Resend em produção; só log sem a chave). */
public interface EmailSender {

	/** Limite do envio em lote do Resend. */
	int MAX_BATCH_SIZE = 100;

	/**
	 * Envia um lote de até {@link #MAX_BATCH_SIZE} e-mails de uma vez: ou vão todos, ou nenhum.
	 *
	 * @throws RuntimeException se o provedor recusar ou estiver fora do ar
	 */
	void sendAll(List<EmailMessage> messages);
}
