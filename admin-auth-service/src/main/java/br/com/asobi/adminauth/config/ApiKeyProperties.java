package br.com.asobi.adminauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param apiKey vazia = não confere (só local) */
@ConfigurationProperties("admin-auth-service")
public record ApiKeyProperties(String apiKey) {
}
