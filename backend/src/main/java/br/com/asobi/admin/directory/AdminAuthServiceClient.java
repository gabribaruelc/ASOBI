package br.com.asobi.admin.directory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;

/** Cliente do admin-auth-service, dono da lista de admins e da conferência do login Google. */
public class AdminAuthServiceClient implements AdminDirectory {

	private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceClient.class);

	// tempo extra, porque o Cloud Run pode estar "acordando" (ele desliga quando ninguém usa)
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	// O painel pergunta "é admin?" a cada requisição; a resposta vale por este tempo.
	// Quando um admin é removido por aqui, essa resposta guardada é apagada na hora.
	private static final Duration CACHE_TTL = Duration.ofSeconds(30);

	private static final String UNAVAILABLE = "Não foi possível falar com o serviço de admins agora. Tente novamente em instantes.";

	private final RestClient restClient;
	private final Map<String, CachedAnswer> cache = new ConcurrentHashMap<>();

	public AdminAuthServiceClient(AdminAuthServiceProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	AdminAuthServiceClient(AdminAuthServiceProperties properties, RestClient.Builder builder) {
		RestClient.Builder configured = builder.baseUrl(properties.url())
				.defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		if (StringUtils.hasText(properties.apiKey())) {
			configured.defaultHeader("X-Api-Key", properties.apiKey());
		}
		this.restClient = configured.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	/** Serviço fora do ar = ninguém entra (nunca libera na dúvida). */
	@Override
	public boolean isAdmin(String email) {
		String normalized = AdminDirectory.normalize(email);
		if (!StringUtils.hasText(normalized)) {
			return false;
		}
		CachedAnswer cached = cache.get(normalized);
		long now = System.nanoTime();
		if (cached != null && now - cached.at() < CACHE_TTL.toNanos()) {
			return cached.admin();
		}
		try {
			Authorization answer = restClient.get()
					.uri(uri -> uri.path("/api/authorizations").queryParam("email", normalized).build())
					.retrieve()
					.body(Authorization.class);
			boolean admin = answer != null && answer.admin();
			cache.put(normalized, new CachedAnswer(admin, now));
			return admin;
		} catch (RestClientException ex) {
			log.error("Serviço de admins indisponível ao conferir o acesso: {}", ex.getMessage());
			return false;
		}
	}

	@Override
	public boolean authorizeGoogleLogin(String idToken, String email) {
		try {
			Authorization answer = restClient.post()
					.uri("/api/logins")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("idToken", idToken))
					.retrieve()
					.body(Authorization.class);
			return answer != null && answer.admin()
					&& AdminDirectory.normalize(email).equals(AdminDirectory.normalize(answer.email()));
		} catch (RestClientResponseException ex) {
			// 401 = token recusado; 403 = e-mail fora da lista
			log.warn("Login Google recusado pelo serviço de admins ({}): {}", ex.getStatusCode(), detailOf(ex));
			return false;
		} catch (RestClientException ex) {
			log.error("Serviço de admins indisponível ao conferir o login: {}", ex.getMessage());
			return false;
		}
	}

	@Override
	public List<AdminAccount> listAdmins() {
		try {
			List<AdminAccount> admins = restClient.get().uri("/api/admins").retrieve()
					.body(new ParameterizedTypeReference<List<AdminAccount>>() {
					});
			return admins == null ? List.of() : admins;
		} catch (RestClientException ex) {
			log.error("Serviço de admins indisponível ao listar: {}", ex.getMessage());
			throw new BusinessException(UNAVAILABLE);
		}
	}

	@Override
	public AdminAccount addAdmin(String email, String createdByEmail) {
		Map<String, String> body = createdByEmail == null
				? Map.of("email", email)
				: Map.of("email", email, "createdByEmail", createdByEmail);
		try {
			AdminAccount created = restClient.post()
					.uri("/api/admins")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(AdminAccount.class);
			cache.clear();
			return created;
		} catch (RestClientException ex) {
			throw translate(ex);
		}
	}

	@Override
	public void removeAdmin(Long id, String currentEmail) {
		try {
			restClient.delete()
					.uri(uri -> uri.path("/api/admins/{id}").queryParam("requestedBy", currentEmail).build(id))
					.retrieve()
					.toBodilessEntity();
			cache.clear();
		} catch (RestClientException ex) {
			throw translate(ex);
		}
	}

	/** 404 e 409 trazem mensagem pronta para o painel; o resto não vaza. */
	private static RuntimeException translate(RestClientException ex) {
		if (ex instanceof RestClientResponseException response) {
			String detail = detailOf(response);
			if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				return new NotFoundException(StringUtils.hasText(detail) ? detail : "Admin não encontrado.");
			}
			if (response.getStatusCode().value() == HttpStatus.CONFLICT.value() && StringUtils.hasText(detail)) {
				return new BusinessException(detail);
			}
			log.warn("Serviço de admins respondeu {}: {}", response.getStatusCode(), detail);
		} else {
			log.error("Falha ao chamar o serviço de admins", ex);
		}
		return new BusinessException(UNAVAILABLE);
	}

	private static String detailOf(RestClientResponseException ex) {
		try {
			ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
			return problem == null ? null : problem.getDetail();
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private record CachedAnswer(boolean admin, long at) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Authorization(String email, boolean admin) {
	}
}
