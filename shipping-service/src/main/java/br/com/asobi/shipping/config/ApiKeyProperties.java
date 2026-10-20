package br.com.asobi.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param apiKey vazia = não confere (só local) */
@ConfigurationProperties("shipping-service")
public record ApiKeyProperties(String apiKey) {
}
