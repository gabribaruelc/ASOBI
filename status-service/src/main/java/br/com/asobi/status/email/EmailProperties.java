package br.com.asobi.status.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resendApiKey chave da API do Resend (re_...); vazio = os e-mails só aparecem no log
 * @param from         remetente; o domínio precisa estar verificado no Resend
 */
@ConfigurationProperties("status.email")
public record EmailProperties(String resendApiKey, String from) {
}
