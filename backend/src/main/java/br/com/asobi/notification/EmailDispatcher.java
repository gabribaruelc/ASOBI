package br.com.asobi.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Só envia o e-mail depois que tudo foi salvo no banco. Assim ninguém recebe "pagamento
 * aprovado" de algo que foi desfeito, e o estoque não fica travado esperando o Resend.
 * Se o e-mail falhar, o pedido continua normal; o erro só vai para o log.
 *
 * O envio espera a resposta (não roda em segundo plano) de propósito: o Cloud Run corta o
 * processador assim que a resposta sai, então uma tarefa em segundo plano poderia nem rodar.
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
