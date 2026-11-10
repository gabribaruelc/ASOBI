package br.com.asobi.notification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Confere se a chave do Resend ainda vale e se o domínio do remetente continua verificado.
 * Aparece como "resend" no /actuator/health, que o status-service lê.
 */
public class ResendHealthIndicator extends AbstractHealthIndicator {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final String fromDomain;

	public ResendHealthIndicator(EmailProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	ResendHealthIndicator(EmailProperties properties, RestClient.Builder builder) {
		super("Resend não respondeu");
		this.fromDomain = domainOf(properties.from());
		this.restClient = builder
				.baseUrl("https://api.resend.com")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resendApiKey())
				.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		Map<String, Object> response;
		try {
			response = restClient.get().uri("/domains").retrieve()
					.body(new ParameterizedTypeReference<Map<String, Object>>() {
					});
		} catch (RestClientResponseException ex) {
			// chave só de envio não pode listar domínios, mas foi aceita
			if (ex.getResponseBodyAsString().contains("restricted_api_key")) {
				builder.up();
				return;
			}
			builder.down().withDetail("error", "Resend recusou a chave (HTTP " + ex.getStatusCode().value() + ")");
			return;
		}
		Object data = response == null ? null : response.get("data");
		if (fromDomain != null && data instanceof List<?> domains) {
			for (Object item : domains) {
				if (item instanceof Map<?, ?> domain && fromDomain.equalsIgnoreCase(String.valueOf(domain.get("name")))
						&& !"verified".equals(domain.get("status"))) {
					builder.down().withDetail("error", "domínio " + fromDomain + " não está verificado");
					return;
				}
			}
		}
		builder.up();
	}

	/** "ASOBI <pedidos@asobi.com.br>" → "asobi.com.br" */
	static String domainOf(String from) {
		if (from == null || !from.contains("@")) {
			return null;
		}
		return from.substring(from.lastIndexOf('@') + 1).replace(">", "").trim();
	}
}
