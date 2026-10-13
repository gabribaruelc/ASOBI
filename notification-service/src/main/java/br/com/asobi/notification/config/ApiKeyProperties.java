package br.com.asobi.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param apiKey vazia = não confere (só local) */
@ConfigurationProperties("notification-service")
public record ApiKeyProperties(String apiKey) {
}
