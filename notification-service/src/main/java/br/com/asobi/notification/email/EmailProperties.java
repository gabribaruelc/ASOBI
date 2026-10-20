package br.com.asobi.notification.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resendApiKey chave da API do Resend (re_...); vazio = os e-mails só aparecem no log
 * @param from         remetente; o domínio precisa estar verificado no Resend
 * @param replyTo      para onde vão as respostas (e-mail de atendimento); opcional
 */
@ConfigurationProperties("notification.email")
public record EmailProperties(String resendApiKey, String from, String replyTo) {
}
