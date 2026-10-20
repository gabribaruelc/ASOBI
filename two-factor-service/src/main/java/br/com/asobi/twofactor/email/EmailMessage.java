package br.com.asobi.twofactor.email;

/** Um e-mail pronto para envio (o remetente vem da configuração). */
public record EmailMessage(String to, String subject, String html, String text) {
}
