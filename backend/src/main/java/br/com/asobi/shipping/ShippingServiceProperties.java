package br.com.asobi.shipping;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    vazio = frete automático indisponível
 * @param apiKey mesma SHIPPING_SERVICE_API_KEY configurada no shipping-service
 */
@ConfigurationProperties("asobi.shipping-service")
public record ShippingServiceProperties(String url, String apiKey) {
}
