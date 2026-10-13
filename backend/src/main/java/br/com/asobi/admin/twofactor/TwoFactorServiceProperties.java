package br.com.asobi.admin.twofactor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    vazio = painel sem segundo fator
 * @param apiKey mesma TWO_FACTOR_SERVICE_API_KEY configurada no two-factor-service
 */
@ConfigurationProperties("asobi.two-factor-service")
public record TwoFactorServiceProperties(String url, String apiKey) {
}
