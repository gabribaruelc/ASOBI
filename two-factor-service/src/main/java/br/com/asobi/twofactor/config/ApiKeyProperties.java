package br.com.asobi.twofactor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param apiKey vazia = não confere (só local) */
@ConfigurationProperties("two-factor-service")
public record ApiKeyProperties(String apiKey) {
}
