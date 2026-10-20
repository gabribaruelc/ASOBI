package br.com.asobi.alert;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import br.com.asobi.common.exception.BusinessException;

/** Cliente do notification-service, dono das inscrições e do envio dos avisos. */
public class NotificationServiceClient implements ProductAlertGateway {

	private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

	// folga para o cold start do Cloud Run e para o envio em lote dos e-mails
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private static final String DISABLED = "Os avisos por e-mail não estão disponíveis no momento.";
	private static final String UNAVAILABLE = "Não foi possível registrar o aviso agora. Tente novamente em instantes.";

	private final RestClient restClient;
	private final boolean hasUrl;

	public NotificationServiceClient(NotificationServiceProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	NotificationServiceClient(NotificationServiceProperties properties, RestClient.Builder builder) {
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
	public void subscribe(String email, Long productId, AlertType type) {
		if (!hasUrl) {
			throw new BusinessException(DISABLED);
		}
		try {
			restClient.post()
					.uri("/api/subscriptions")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("email", email, "productId", productId, "type", type, "consent", true))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			log.error("Falha ao registrar o aviso {} do produto {}: {}", type, productId, ex.getMessage());
			throw new BusinessException(UNAVAILABLE);
		}
	}

	@Override
	public void publish(Alert alert) {
		if (!hasUrl) {
			return;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", alert.type());
		body.put("productId", alert.productId());
		body.put("productName", alert.productName());
		body.put("productUrl", alert.productUrl());
		body.put("price", alert.price());
		if (alert.originalPrice() != null) {
			body.put("originalPrice", alert.originalPrice());
		}
		Result result = restClient.post()
				.uri("/api/product-events")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(Result.class);
		if (result != null) {
			log.info("Aviso {} do produto {}: {} enviado(s), {} com falha, {} já avisado(s) há pouco",
					alert.type(), alert.productId(), result.sent(), result.failed(), result.skipped());
		}
	}

	@Override
	public void removeProduct(Long productId) {
		if (!hasUrl) {
			return;
		}
		restClient.delete()
				.uri(uri -> uri.path("/api/subscriptions").queryParam("productId", productId).build())
				.retrieve()
				.toBodilessEntity();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Result(int subscribers, int sent, int failed, int skipped) {
	}
}
