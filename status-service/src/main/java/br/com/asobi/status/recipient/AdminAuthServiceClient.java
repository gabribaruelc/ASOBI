package br.com.asobi.status.recipient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lê a lista de admins em GET /api/admins do admin-auth-service. */
@Component
public class AdminAuthServiceClient implements AdminListClient {

	// folga para o Cloud Run "acordar"
	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	private final RestClient restClient;
	private final boolean configured;

	public AdminAuthServiceClient(AdminAuthServiceProperties properties) {
		this.configured = StringUtils.hasText(properties.url());
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		RestClient.Builder builder = RestClient.builder()
				.requestFactory(factory)
				.defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		if (configured) {
			builder.baseUrl(properties.url());
		}
		if (StringUtils.hasText(properties.apiKey())) {
			builder.defaultHeader("X-Api-Key", properties.apiKey());
		}
		this.restClient = builder.build();
	}

	@Override
	public boolean isConfigured() {
		return configured;
	}

	@Override
	public List<String> listAdminEmails() {
		List<Admin> admins = restClient.get()
				.uri("/api/admins")
				.retrieve()
				.body(new ParameterizedTypeReference<List<Admin>>() {
				});
		return admins == null ? List.of()
				: admins.stream().map(Admin::email).filter(Objects::nonNull).toList();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Admin(String email) {
	}
}
