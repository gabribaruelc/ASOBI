package br.com.asobi.notification;

import java.util.List;

/** Quem de fato entrega o e-mail (Resend em produção; só log sem a chave). */
public interface EmailSender {

	/** Limite do envio em lote do Resend. */
	int MAX_BATCH_SIZE = 100;

	/** @throws RuntimeException se o provedor recusar ou estiver fora do ar */
	void send(EmailMessage message);

	/**
	 * Envia até {@link #MAX_BATCH_SIZE} e-mails numa chamada só: ou vão todos, ou nenhum.
	 *
	 * @throws RuntimeException se o provedor recusar ou estiver fora do ar
	 */
	void sendAll(List<EmailMessage> messages);
}
