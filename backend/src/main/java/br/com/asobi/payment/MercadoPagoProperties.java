package br.com.asobi.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param accessToken   credencial do Mercado Pago (TEST-... no sandbox, APP_USR-... em produção);
 *                      vazio = modo manual
 * @param webhookSecret "assinatura secreta" das notificações (painel do Mercado Pago → Webhooks)
 */
@ConfigurationProperties("asobi.mercado-pago")
public record MercadoPagoProperties(String accessToken, String webhookSecret) {
}
