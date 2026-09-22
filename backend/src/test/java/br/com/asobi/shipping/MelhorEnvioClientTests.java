package br.com.asobi.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.asobi.common.exception.BusinessException;

class MelhorEnvioClientTests {

	private static final String URL = "https://sandbox.melhorenvio.com.br/api/v2/me/shipment/calculate";

	private static final ShippingQuoteProvider.QuoteRequest REQUEST = new ShippingQuoteProvider.QuoteRequest(
			"01310-100", "20040-002", List.of(new ShippingQuoteProvider.Parcel("corrida-dos-sapos", 2,
					new BigDecimal("89.90"), new BigDecimal("1.000"), 30, 8, 30)));

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final MelhorEnvioClient client = new MelhorEnvioClient(
			new MelhorEnvioProperties("tok-123", true, "dev@asobi.com.br"), builder);

	@Test
	void parsesAvailableServicesSortedByPrice() {
		server.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer tok-123"))
				.andExpect(header("User-Agent", "ASOBI (dev@asobi.com.br)"))
				.andExpect(jsonPath("$.from.postal_code").value("01310100"))
				.andExpect(jsonPath("$.to.postal_code").value("20040002"))
				.andExpect(jsonPath("$.products[0].quantity").value(2))
				.andExpect(jsonPath("$.products[0].insurance_value").value(89.90))
				.andRespond(withSuccess("""
						[
						  {"id": 2, "name": "SEDEX", "price": "41.20", "custom_price": "39.90",
						   "delivery_time": 2, "custom_delivery_time": 3, "company": {"id": 1, "name": "Correios"}},
						  {"id": 1, "name": "PAC", "price": "25.50", "delivery_time": 6, "company": {"name": "Correios"}},
						  {"id": 17, "name": "Mini Envios", "error": "Serviço indisponível para o trecho.",
						   "company": {"name": "Correios"}}
						]
						""", MediaType.APPLICATION_JSON));

		List<ShippingOption> options = client.quote(REQUEST);

		assertThat(options).extracting(ShippingOption::id).containsExactly("1", "2");
		assertThat(options.get(0).price()).isEqualByComparingTo("25.50");
		assertThat(options.get(1).price()).as("usa o custom_price").isEqualByComparingTo("39.90");
		assertThat(options.get(1).deliveryDays()).isEqualTo(3);
		assertThat(options.get(0).label()).isEqualTo("Correios PAC · 6 dias úteis");
		server.verify();
	}

	@Test
	void turnsApiFailureIntoFriendlyError() {
		server.expect(requestTo(URL)).andRespond(withServerError());
		assertThatThrownBy(() -> client.quote(REQUEST))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Não foi possível calcular o frete agora");
	}

	@Test
	void isNotConfiguredWithoutToken() {
		MelhorEnvioClient withoutToken = new MelhorEnvioClient(new MelhorEnvioProperties("", true, null));
		assertThat(withoutToken.isConfigured()).isFalse();
		assertThatThrownBy(() -> withoutToken.quote(REQUEST)).isInstanceOf(BusinessException.class);
	}
}
