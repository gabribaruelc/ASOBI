package br.com.asobi.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.asobi.alert.ProductAlertGateway.Alert;
import br.com.asobi.common.exception.BusinessException;

class NotificationServiceClientTests {

	private static final String BASE = "http://notification.test";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final NotificationServiceClient client = new NotificationServiceClient(
			new NotificationServiceProperties(BASE, "key-123"), builder);

	@Test
	void subscribesWithConsent() {
		server.expect(requestTo(BASE + "/api/subscriptions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Api-Key", "key-123"))
				.andExpect(jsonPath("$.email").value("maria@example.com"))
				.andExpect(jsonPath("$.productId").value(7))
				.andExpect(jsonPath("$.type").value("BACK_IN_STOCK"))
				.andExpect(jsonPath("$.consent").value(true))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"id\": 1}"));

		client.subscribe("maria@example.com", 7L, AlertType.BACK_IN_STOCK);

		server.verify();
	}

	@Test
	void subscribeFailureBecomesFriendlyMessage() {
		server.expect(requestTo(BASE + "/api/subscriptions")).andRespond(withServerError());

		assertThatThrownBy(() -> client.subscribe("maria@example.com", 7L, AlertType.PROMOTION))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Não foi possível registrar o aviso agora. Tente novamente em instantes.");
	}

	@Test
	void publishesTheEventWithEverythingTheEmailNeeds() {
		server.expect(requestTo(BASE + "/api/product-events"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.type").value("PROMOTION"))
				.andExpect(jsonPath("$.productId").value(7))
				.andExpect(jsonPath("$.productName").value("Corrida dos Sapos"))
				.andExpect(jsonPath("$.productUrl").value("https://asobi.com.br/jogos/corrida-dos-sapos"))
				.andExpect(jsonPath("$.price").value(79.90))
				.andExpect(jsonPath("$.originalPrice").value(99.90))
				.andRespond(withSuccess("{\"subscribers\": 2, \"sent\": 2, \"failed\": 0, \"skipped\": 0}",
						MediaType.APPLICATION_JSON));

		client.publish(new Alert(AlertType.PROMOTION, 7L, "Corrida dos Sapos",
				"https://asobi.com.br/jogos/corrida-dos-sapos", new BigDecimal("79.90"), new BigDecimal("99.90"), null));

		server.verify();
	}

	@Test
	void publishFailureIsThrownForTheListenerToLog() {
		server.expect(requestTo(BASE + "/api/product-events")).andRespond(withServerError());

		assertThatThrownBy(() -> client.publish(new Alert(AlertType.BACK_IN_STOCK, 7L, "Corrida dos Sapos",
				"https://asobi.com.br/jogos/corrida-dos-sapos", new BigDecimal("99.90"), null, null)))
				.isInstanceOf(RestClientException.class);
	}

	@Test
	void removesTheSubscriptionsOfADeletedProduct() {
		server.expect(requestTo(BASE + "/api/subscriptions?productId=7"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withSuccess("{\"removed\": 3}", MediaType.APPLICATION_JSON));

		client.removeProduct(7L);

		server.verify();
	}

	@Test
	void withoutUrlNothingIsCalled() {
		NotificationServiceClient disabled = new NotificationServiceClient(new NotificationServiceProperties("", null));

		assertThat(disabled.isEnabled()).isFalse();
		assertThatThrownBy(() -> disabled.subscribe("maria@example.com", 7L, AlertType.PROMOTION))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Os avisos por e-mail não estão disponíveis no momento.");
		disabled.removeProduct(7L);
	}
}
