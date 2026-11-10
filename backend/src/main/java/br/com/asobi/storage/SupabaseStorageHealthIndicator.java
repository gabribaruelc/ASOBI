package br.com.asobi.storage;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Confere se o Supabase Storage aceita a chave secreta. Bucket inexistente conta como no ar:
 * ele é criado no envio da primeira foto. Aparece como "supabaseStorage" no /actuator/health.
 */
public class SupabaseStorageHealthIndicator extends AbstractHealthIndicator {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final String bucket;

	public SupabaseStorageHealthIndicator(StorageProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	SupabaseStorageHealthIndicator(StorageProperties properties, RestClient.Builder builder) {
		super("Supabase Storage não respondeu");
		this.bucket = properties.bucket();
		this.restClient = builder
				.baseUrl(properties.supabaseUrl().trim().replaceAll("/+$", "") + "/storage/v1")
				.defaultHeader("apikey", properties.supabaseKey())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.supabaseKey())
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
			restClient.get().uri("/bucket/" + bucket).retrieve().toBodilessEntity();
			builder.up();
		} catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			if (status == 401 || status == 403 || ex.getStatusCode().is5xxServerError()) {
				builder.down().withDetail("error", "Supabase Storage respondeu HTTP " + status);
			} else {
				builder.up();
			}
		}
	}
}
