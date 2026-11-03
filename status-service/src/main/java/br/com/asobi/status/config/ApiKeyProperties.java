package br.com.asobi.status.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param apiKey vazia = não confere (só local) */
@ConfigurationProperties("status-service")
public record ApiKeyProperties(String apiKey) {
}
