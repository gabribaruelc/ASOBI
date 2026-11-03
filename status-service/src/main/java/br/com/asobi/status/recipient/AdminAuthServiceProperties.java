package br.com.asobi.status.recipient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    endereço do admin-auth-service; vazio = avisa só o fallback-emails
 * @param apiKey mesma ADMIN_AUTH_SERVICE_API_KEY configurada nele
 */
@ConfigurationProperties("status.admin-auth-service")
public record AdminAuthServiceProperties(String url, String apiKey) {
}
