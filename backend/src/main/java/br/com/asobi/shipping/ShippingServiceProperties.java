package br.com.asobi.shipping;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    endereço do microsserviço de frete (vazio = frete automático indisponível)
 * @param apiKey chave enviada no cabeçalho X-Api-Key (a mesma SHIPPING_SERVICE_API_KEY do serviço)
 */
@ConfigurationProperties("asobi.shipping-service")
public record ShippingServiceProperties(String url, String apiKey) {
}
