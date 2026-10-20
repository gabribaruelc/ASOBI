package br.com.asobi.notification.email;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sem RESEND_API_KEY (desenvolvimento): não envia nada, só registra no log. */
public class LogEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

	@Override
	public void sendAll(List<EmailMessage> messages) {
		for (EmailMessage message : messages) {
			log.info("E-mail não enviado (sem RESEND_API_KEY) para {}: {} (descadastro: {})", message.to(),
					message.subject(), message.unsubscribeUrl());
		}
	}
}
