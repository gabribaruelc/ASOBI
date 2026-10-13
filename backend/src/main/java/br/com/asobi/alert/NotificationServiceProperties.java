package br.com.asobi.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url    vazio = avisos de produto desligados
 * @param apiKey mesma NOTIFICATION_SERVICE_API_KEY configurada no notification-service
 */
@ConfigurationProperties("asobi.notification-service")
public record NotificationServiceProperties(String url, String apiKey) {
}
