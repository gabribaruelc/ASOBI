package br.com.asobi.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.asobi.common.exception.BusinessException;

class ShippingServiceClientTests {

	private static final String BASE = "http://shipping.test";

	private static final ShippingQuoteProvider.QuoteRequest REQUEST = new ShippingQuoteProvider.QuoteRequest(
			"01310-100", "20040-002", List.of(new ShippingQuoteProvider.Parcel("corrida-dos-sapos", 2,
					new BigDecimal("89.90"), new BigDecimal("1.000"), 30, 8, 30)));

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final ShippingServiceClient client = new ShippingServiceClient(
			new ShippingServiceProperties(BASE, "key-123"), builder);

	@Test
	void sendsParcelsAndMapsOptions() {
		server.expect(requestTo(BASE + "/api/quotes"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Api-Key", "key-123"))
				.andExpect(jsonPath("$.originPostalCode").value("01310-100"))
				.andExpect(jsonPath("$.destinationPostalCode").value("20040-002"))
				.andExpect(jsonPath("$.parcels[0].id").value("corrida-dos-sapos"))
				.andExpect(jsonPath("$.parcels[0].quantity").value(2))
				.andRespond(withSuccess("""
						[{"id": "1", "name": "PAC", "company": "Correios", "price": 25.50, "deliveryDays": 6}]
						""", MediaType.APPLICATION_JSON));

		List<ShippingOption> options = client.quote(REQUEST);

		assertThat(options).hasSize(1);
		assertThat(options.get(0).price()).isEqualByComparingTo("25.50");
		assertThat(options.get(0).originalPrice()).isNull();
		assertThat(options.get(0).label()).isEqualTo("Correios PAC · 6 dias úteis");
		server.verify();
	}

	@Test
	void forwardsCarrierFailureMessage() {
		server.expect(requestTo(BASE + "/api/quotes")).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body("{\"status\": 502, \"detail\": \"Não foi possível calcular o frete agora. Tente de novo.\"}"));
		assertThatThrownBy(() -> client.quote(REQUEST))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Não foi possível calcular o frete agora. Tente de novo.");
	}

	@Test
	void hidesClientErrorsBehindGenericMessage() {
		server.expect(requestTo(BASE + "/api/quotes")).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body("{\"status\": 401, \"detail\": \"Chave de API inválida.\"}"));
		assertThatThrownBy(() -> client.quote(REQUEST))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Não foi possível calcular o frete agora");
	}

	@Test
	void isConfiguredWhenServiceReportsIt() {
		server.expect(requestTo(BASE + "/api/provider"))
				.andRespond(withSuccess("{\"name\": \"melhor-envio\", \"configured\": true}", MediaType.APPLICATION_JSON));
		assertThat(client.isConfigured()).isTrue();
	}

	@Test
	void isNotConfiguredWhenServiceIsDown() {
		server.expect(requestTo(BASE + "/api/provider")).andRespond(withResourceNotFound());
		assertThat(client.isConfigured()).isFalse();
	}

	@Test
	void isNotConfiguredWithoutUrl() {
		ShippingServiceClient withoutUrl = new ShippingServiceClient(new ShippingServiceProperties("", null));
		assertThat(withoutUrl.isConfigured()).isFalse();
		assertThatThrownBy(() -> withoutUrl.quote(REQUEST)).isInstanceOf(BusinessException.class);
	}
}
