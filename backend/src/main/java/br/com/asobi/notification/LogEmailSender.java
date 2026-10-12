package br.com.asobi.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sem RESEND_API_KEY (desenvolvimento): não envia nada, só registra no log. */
public class LogEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

	@Override
	public void send(EmailMessage message) {
		log.info("E-mail não enviado (sem RESEND_API_KEY) para {}: {}", message.to(), message.subject());
	}
}
