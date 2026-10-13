package br.com.asobi.notification.email;

/**
 * Um e-mail pronto para envio (o remetente vem da configuração).
 *
 * @param unsubscribeUrl vai também no cabeçalho List-Unsubscribe, para o botão de descadastro
 *                       do próprio programa de e-mail
 */
public record EmailMessage(String to, String subject, String html, String unsubscribeUrl) {
}
