package br.com.asobi.notification.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param publicUrl endereço público deste serviço, usado no link de descadastro dos e-mails
 * @param storeUrl  endereço da loja, para o botão de voltar da página de descadastro
 * @param cooldown  intervalo mínimo entre dois avisos iguais para a mesma pessoa
 */
@ConfigurationProperties("notification")
public record NotificationProperties(String publicUrl, String storeUrl, Duration cooldown) {

	public NotificationProperties {
		publicUrl = publicUrl == null ? null : publicUrl.replaceAll("/+$", "");
		storeUrl = storeUrl == null ? null : storeUrl.replaceAll("/+$", "");
	}
}
