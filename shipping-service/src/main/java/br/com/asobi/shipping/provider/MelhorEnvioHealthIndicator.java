package br.com.asobi.shipping.provider;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.web.client.RestClientResponseException;

/** Aparece como "melhorEnvio" no /actuator/health, que o status-service lê. */
public class MelhorEnvioHealthIndicator extends AbstractHealthIndicator {

	private final MelhorEnvioClient client;

	public MelhorEnvioHealthIndicator(MelhorEnvioClient client) {
		super("Melhor Envio não respondeu");
		this.client = client;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		try {
			client.checkAccess();
			builder.up();
		} catch (RestClientResponseException ex) {
			builder.down().withDetail("error", "Melhor Envio recusou o token (HTTP " + ex.getStatusCode().value() + ")");
		}
	}
}
