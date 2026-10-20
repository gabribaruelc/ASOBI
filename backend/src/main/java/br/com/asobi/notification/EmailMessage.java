package br.com.asobi.notification;

import java.util.List;

/**
 * Um e-mail pronto para envio (o remetente vem da configuração).
 *
 * @param unsubscribeUrl só em e-mail de marketing (newsletter): vai também no cabeçalho
 *                       List-Unsubscribe, para o botão de descadastro do programa de e-mail
 */
public record EmailMessage(List<String> to, String subject, String html, String unsubscribeUrl) {

	public EmailMessage(List<String> to, String subject, String html) {
		this(to, subject, html, null);
	}
}
