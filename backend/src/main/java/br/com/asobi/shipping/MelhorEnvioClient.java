package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.asobi.common.exception.BusinessException;

/** Cotação no Melhor Envio (POST /api/v2/me/shipment/calculate). */
public class MelhorEnvioClient implements ShippingQuoteProvider {

	private static final Logger log = LoggerFactory.getLogger(MelhorEnvioClient.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(8);

	private final MelhorEnvioProperties properties;
	private final RestClient restClient;

	public MelhorEnvioClient(MelhorEnvioProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	/** Construtor para testes (MockRestServiceServer). */
	MelhorEnvioClient(MelhorEnvioProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		String baseUrl = properties.sandbox() ? "https://sandbox.melhorenvio.com.br" : "https://melhorenvio.com.br";
		String contact = StringUtils.hasText(properties.contactEmail()) ? properties.contactEmail() : "contato@asobi.com.br";
		this.restClient = builder
				.baseUrl(baseUrl)
				.defaultHeader("Authorization", "Bearer " + properties.token())
				.defaultHeader("User-Agent", "ASOBI (" + contact + ")")
				.defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	@Override
	public boolean isConfigured() {
		return StringUtils.hasText(properties.token());
	}

	@Override
	public List<ShippingOption> quote(QuoteRequest request) {
		if (!isConfigured()) {
			throw new BusinessException("Cálculo de frete indisponível no momento.");
		}
		Map<String, Object> body = Map.of(
				"from", Map.of("postal_code", digits(request.originPostalCode())),
				"to", Map.of("postal_code", digits(request.destinationPostalCode())),
				"products", request.parcels().stream().map(parcel -> Map.of(
						"id", parcel.id(),
						"width", parcel.widthCm(),
						"height", parcel.heightCm(),
						"length", parcel.lengthCm(),
						"weight", parcel.weightKg(),
						"insurance_value", parcel.unitValue(),
						"quantity", parcel.quantity())).toList());
		try {
			List<Service> services = restClient.post()
					.uri("/api/v2/me/shipment/calculate")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(new ParameterizedTypeReference<List<Service>>() {
					});
			return services == null ? List.of() : services.stream()
					.filter(Service::isAvailable)
					.map(Service::toOption)
					.sorted(Comparator.comparing(ShippingOption::price))
					.toList();
		} catch (RestClientException ex) {
			log.error("Falha ao cotar frete no Melhor Envio", ex);
			throw new BusinessException("Não foi possível calcular o frete agora. Tente novamente em instantes.");
		}
	}

	private static String digits(String postalCode) {
		return postalCode.replaceAll("\\D", "");
	}

	/** Um serviço na resposta do Melhor Envio (campos que usamos). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Service(
			Integer id,
			String name,
			String price,
			@JsonProperty("custom_price") String customPrice,
			@JsonProperty("delivery_time") Integer deliveryTime,
			@JsonProperty("custom_delivery_time") Integer customDeliveryTime,
			Company company,
			String error) {

		boolean isAvailable() {
			return error == null && (StringUtils.hasText(customPrice) || StringUtils.hasText(price));
		}

		ShippingOption toOption() {
			BigDecimal value = new BigDecimal(StringUtils.hasText(customPrice) ? customPrice : price);
			return new ShippingOption(String.valueOf(id), name, company == null ? null : company.name(), value, null,
					customDeliveryTime != null ? customDeliveryTime : deliveryTime);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Company(String name) {
	}
}
