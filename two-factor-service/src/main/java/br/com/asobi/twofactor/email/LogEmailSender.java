package br.com.asobi.twofactor.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sem RESEND_API_KEY (desenvolvimento): não envia nada e escreve o e-mail no log, com o
 * código, para dar para testar o fluxo. Em produção a chave é obrigatória.
 */
public class LogEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

	@Override
	public void send(EmailMessage message) {
		log.info("E-mail não enviado (sem RESEND_API_KEY) para {}: {}\n{}", message.to(), message.subject(),
				message.text());
	}
}
