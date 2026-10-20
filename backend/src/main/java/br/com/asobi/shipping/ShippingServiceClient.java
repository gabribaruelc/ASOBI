package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.asobi.common.exception.BusinessException;

/** Cliente do shipping-service. O token do Melhor Envio fica só lá. */
public class ShippingServiceClient implements ShippingQuoteProvider {

	private static final Logger log = LoggerFactory.getLogger(ShippingServiceClient.class);

	// folga para o cold start do Cloud Run
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private static final String UNAVAILABLE = "Não foi possível calcular o frete agora. Tente novamente em instantes.";

	private final RestClient restClient;
	private final boolean hasUrl;

	public ShippingServiceClient(ShippingServiceProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	ShippingServiceClient(ShippingServiceProperties properties, RestClient.Builder builder) {
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
	public boolean isConfigured() {
		if (!hasUrl) {
			return false;
		}
		try {
			ProviderStatus status = restClient.get().uri("/api/provider").retrieve().body(ProviderStatus.class);
			return status != null && status.configured();
		} catch (RestClientException ex) {
			log.warn("Serviço de frete indisponível ao consultar a configuração: {}", ex.getMessage());
			return false;
		}
	}

	@Override
	public List<ShippingOption> quote(QuoteRequest request) {
		if (!hasUrl) {
			throw new BusinessException("Cálculo de frete indisponível no momento.");
		}
		try {
			List<RemoteOption> options = restClient.post()
					.uri("/api/quotes")
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(new ParameterizedTypeReference<List<RemoteOption>>() {
					});
			return options == null ? List.of() : options.stream().map(RemoteOption::toOption).toList();
		} catch (RestClientResponseException ex) {
			// 5xx traz mensagem pronta para o cliente; 4xx é erro nosso (chave, payload) e não vaza.
			ProblemDetail problem = readProblem(ex);
			String detail = problem == null ? null : problem.getDetail();
			log.warn("Serviço de frete respondeu {}: {}", ex.getStatusCode(), detail);
			boolean showDetail = ex.getStatusCode().is5xxServerError() && StringUtils.hasText(detail);
			throw new BusinessException(showDetail ? detail : UNAVAILABLE);
		} catch (RestClientException ex) {
			log.error("Falha ao chamar o serviço de frete", ex);
			throw new BusinessException(UNAVAILABLE);
		}
	}

	private static ProblemDetail readProblem(RestClientResponseException ex) {
		try {
			return ex.getResponseBodyAs(ProblemDetail.class);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ProviderStatus(String name, boolean configured) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record RemoteOption(String id, String name, String company, BigDecimal price, Integer deliveryDays) {

		ShippingOption toOption() {
			return new ShippingOption(id, name, company, price, null, deliveryDays);
		}
	}
}
