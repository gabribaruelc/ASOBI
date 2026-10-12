package br.com.asobi.notification;

/** Quem de fato entrega o e-mail (Resend em produção; só log sem a chave). */
public interface EmailSender {

	/** @throws RuntimeException se o provedor recusar ou estiver fora do ar */
	void send(EmailMessage message);
}
