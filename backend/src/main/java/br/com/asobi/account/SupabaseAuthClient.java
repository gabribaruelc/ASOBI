package br.com.asobi.account;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Confere o login do cliente perguntando ao próprio Supabase Auth quem é o dono do token
 * (GET /auth/v1/user). Assim o backend não depende de como o Supabase assina os tokens.
 */
@Component
public class SupabaseAuthClient {

	private static final Logger log = LoggerFactory.getLogger(SupabaseAuthClient.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final boolean configured;

	@Autowired
	public SupabaseAuthClient(CustomerAuthProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	/** Construtor para testes (MockRestServiceServer). */
	SupabaseAuthClient(CustomerAuthProperties properties, RestClient.Builder builder) {
		this.configured = StringUtils.hasText(properties.supabaseUrl())
				&& StringUtils.hasText(properties.supabaseKey());
		if (configured) {
			builder.baseUrl(properties.supabaseUrl().trim().replaceAll("/+$", "") + "/auth/v1")
					.defaultHeader("apikey", properties.supabaseKey());
		} else {
			log.warn("Login de cliente desligado (SUPABASE_URL/SUPABASE_SERVICE_KEY não definidos).");
		}
		this.restClient = builder.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	/** @return o cliente dono do token, ou vazio se o token não vale (ou o login está desligado) */
	public Optional<CustomerIdentity> verify(String accessToken) {
		if (!configured || !StringUtils.hasText(accessToken)) {
			return Optional.empty();
		}
		try {
			SupabaseUser user = restClient.get()
					.uri("/user")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.retrieve()
					.body(SupabaseUser.class);
			if (user == null || user.id() == null || !StringUtils.hasText(user.email())) {
				return Optional.empty();
			}
			return Optional.of(new CustomerIdentity(user.id(), user.email().trim().toLowerCase(Locale.ROOT),
					user.emailConfirmedAt() != null));
		} catch (RestClientResponseException ex) {
			// 401/403: token vencido ou inválido — é o caso normal de "não está logado".
			if (!ex.getStatusCode().is4xxClientError()) {
				log.warn("Supabase Auth respondeu {} ao conferir o login", ex.getStatusCode());
			}
			return Optional.empty();
		} catch (RestClientException ex) {
			log.warn("Supabase Auth indisponível ao conferir o login: {}", ex.getMessage());
			return Optional.empty();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SupabaseUser(UUID id, String email, @JsonProperty("email_confirmed_at") String emailConfirmedAt) {
	}
}
