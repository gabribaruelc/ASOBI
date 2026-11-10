package br.com.asobi.payment;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Confere se o Access Token do Mercado Pago ainda vale (GET /users/me).
 * Aparece como "mercadoPago" no /actuator/health, que o status-service lê.
 */
public class MercadoPagoHealthIndicator extends AbstractHealthIndicator {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;

	public MercadoPagoHealthIndicator(String accessToken) {
		this(accessToken, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	MercadoPagoHealthIndicator(String accessToken, RestClient.Builder builder) {
		super("Mercado Pago não respondeu");
		this.restClient = builder
				.baseUrl("https://api.mercadopago.com")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
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
		try {
			restClient.get().uri("/users/me").retrieve().toBodilessEntity();
			builder.up();
		} catch (RestClientResponseException ex) {
			builder.down().withDetail("error", "Mercado Pago recusou o token (HTTP " + ex.getStatusCode().value() + ")");
		}
	}
}
