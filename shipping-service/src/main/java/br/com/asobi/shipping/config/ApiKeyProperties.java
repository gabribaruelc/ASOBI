package br.com.asobi.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param apiKey chave que o backend da loja envia no cabeçalho X-Api-Key.
 *               Vazia = sem conferência (só para rodar localmente).
 */
@ConfigurationProperties("shipping-service")
public record ApiKeyProperties(String apiKey) {
}
