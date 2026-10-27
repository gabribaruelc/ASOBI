package br.com.asobi.admin.twofactor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.common.exception.BusinessException;

/** Cliente do two-factor-service, que gera, envia e confere o código do painel. */
public class TwoFactorServiceClient implements TwoFactorGateway {

	private static final Logger log = LoggerFactory.getLogger(TwoFactorServiceClient.class);

	/** Um código pedido para outra finalidade não abre o painel. */
	static final String PURPOSE = "admin-login";

	// tempo extra, porque o Cloud Run pode estar "acordando" (ele desliga quando ninguém usa)
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private static final String UNAVAILABLE = "Não foi possível falar com o serviço de verificação agora. Tente novamente em instantes.";
	private static final String EXPIRED = "Este código expirou ou já foi usado. Peça um novo.";

	private final RestClient restClient;
	private final boolean hasUrl;

	public TwoFactorServiceClient(TwoFactorServiceProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	TwoFactorServiceClient(TwoFactorServiceProperties properties, RestClient.Builder builder) {
		this.hasUrl = StringUtils.hasText(properties.url());
		RestClient.Builder configured = builder.defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		if (hasUrl) {
			configured.baseUrl(properties.url());
		}
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

	@Override
	public boolean isEnabled() {
		return hasUrl;
	}

	@Override
	public String requestCode(String email) {
		try {
			Challenge challenge = restClient.post()
					.uri("/api/challenges")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("email", email, "purpose", PURPOSE))
					.retrieve()
					.body(Challenge.class);
			if (challenge == null || !StringUtils.hasText(challenge.id())) {
				throw new BusinessException(UNAVAILABLE);
			}
			return challenge.id();
		} catch (RestClientResponseException ex) {
			ProblemDetail problem = readProblem(ex);
			String detail = problem == null ? null : problem.getDetail();
			log.warn("Serviço de verificação respondeu {} ao pedir o código: {}", ex.getStatusCode(), detail);
			// 429 (muitos pedidos) e 503 (e-mail não saiu) trazem mensagem pronta para a tela
			boolean showDetail = StringUtils.hasText(detail) && (is(ex, HttpStatus.TOO_MANY_REQUESTS)
					|| is(ex, HttpStatus.SERVICE_UNAVAILABLE));
			throw new BusinessException(showDetail ? detail : UNAVAILABLE);
		} catch (RestClientException ex) {
			log.error("Serviço de verificação indisponível ao pedir o código: {}", ex.getMessage());
			throw new BusinessException(UNAVAILABLE);
		}
	}

	@Override
	public Verification verify(String challengeId, String code, String email) {
		try {
			Verified answer = restClient.post()
					.uri("/api/challenges/{id}/verifications", challengeId)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("code", code))
					.retrieve()
					.body(Verified.class);
			boolean matches = answer != null && answer.verified() && PURPOSE.equals(answer.purpose())
					&& AdminDirectory.normalize(email).equals(AdminDirectory.normalize(answer.email()));
			if (!matches) {
				log.warn("Serviço de verificação confirmou um código que não é deste login.");
				return new Verification(Verification.Status.EXPIRED, EXPIRED);
			}
			return new Verification(Verification.Status.VERIFIED, null);
		} catch (RestClientResponseException ex) {
			ProblemDetail problem = readProblem(ex);
			if (is(ex, HttpStatus.UNPROCESSABLE_ENTITY)) {
				return wrongCode(problem);
			}
			if (is(ex, HttpStatus.GONE) || is(ex, HttpStatus.NOT_FOUND)) {
				return new Verification(Verification.Status.EXPIRED, EXPIRED);
			}
			log.warn("Serviço de verificação respondeu {} ao conferir o código: {}", ex.getStatusCode(),
					problem == null ? null : problem.getDetail());
			return new Verification(Verification.Status.UNAVAILABLE, UNAVAILABLE);
		} catch (RestClientException ex) {
			log.error("Serviço de verificação indisponível ao conferir o código: {}", ex.getMessage());
			return new Verification(Verification.Status.UNAVAILABLE, UNAVAILABLE);
		}
	}

	private static Verification wrongCode(ProblemDetail problem) {
		Object attemptsLeft = problem == null || problem.getProperties() == null
				? null
				: problem.getProperties().get("attemptsLeft");
		if (attemptsLeft instanceof Number left) {
			if (left.intValue() <= 0) {
				return new Verification(Verification.Status.EXPIRED,
						"Código incorreto e sem tentativas restantes. Peça um novo.");
			}
			return new Verification(Verification.Status.WRONG_CODE, "Código incorreto. "
					+ (left.intValue() == 1 ? "Você tem mais 1 tentativa." : "Você tem mais " + left + " tentativas."));
		}
		return new Verification(Verification.Status.WRONG_CODE, "Código incorreto.");
	}

	private static boolean is(RestClientResponseException ex, HttpStatus status) {
		return ex.getStatusCode().value() == status.value();
	}

	private static ProblemDetail readProblem(RestClientResponseException ex) {
		try {
			return ex.getResponseBodyAs(ProblemDetail.class);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Challenge(String id) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Verified(String email, String purpose, boolean verified) {
	}
}
