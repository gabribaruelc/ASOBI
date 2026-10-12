package br.com.asobi.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param accessToken   TEST-... (sandbox) ou APP_USR-...; vazio = modo manual
 * @param webhookSecret assinatura secreta configurada em Webhooks no Mercado Pago
 */
@ConfigurationProperties("asobi.mercado-pago")
public record MercadoPagoProperties(String accessToken, String webhookSecret) {
}
