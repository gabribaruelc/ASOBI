package br.com.asobi.admin.directory;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    vazio = lista de admins em memória (só desenvolvimento)
 * @param apiKey mesma ADMIN_AUTH_SERVICE_API_KEY configurada no admin-auth-service
 */
@ConfigurationProperties("asobi.admin-auth-service")
public record AdminAuthServiceProperties(String url, String apiKey) {
}
