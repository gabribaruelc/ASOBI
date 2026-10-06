package br.com.asobi.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resendApiKey chave da API do Resend (re_...); vazio = os e-mails só aparecem no log
 * @param from         remetente, ex.: {@code ASOBI <pedidos@asobi.com.br>} (o domínio precisa estar
 *                     verificado no Resend)
 * @param replyTo      para onde vão as respostas do cliente (e-mail de atendimento); opcional
 */
@ConfigurationProperties("asobi.email")
public record EmailProperties(String resendApiKey, String from, String replyTo) {
}
