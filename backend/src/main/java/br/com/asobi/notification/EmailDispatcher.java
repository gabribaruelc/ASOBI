package br.com.asobi.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envia o e-mail só depois que a transação atual é confirmada: assim ninguém recebe
 * "pagamento aprovado" de uma operação que foi desfeita, e a chamada ao provedor não
 * segura a trava do estoque. Falha de e-mail nunca derruba o pedido — só vai para o log.
 *
 * O envio é síncrono de propósito: no Cloud Run a CPU é cortada assim que a resposta
 * sai, então uma tarefa em segundo plano poderia nunca rodar.
 */
@Component
public class EmailDispatcher {

	private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

	private final EmailSender emailSender;

	public EmailDispatcher(EmailSender emailSender) {
		this.emailSender = emailSender;
	}

	public void sendAfterCommit(EmailMessage message) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			sendQuietly(message);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sendQuietly(message);
			}
		});
	}

	private void sendQuietly(EmailMessage message) {
		try {
			emailSender.send(message);
		} catch (RuntimeException ex) {
			log.error("Falha ao enviar o e-mail \"{}\"", message.subject(), ex);
		}
	}
}
