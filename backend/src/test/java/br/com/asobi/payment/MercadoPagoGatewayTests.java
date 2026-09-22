package br.com.asobi.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.mercadopago.client.preference.PreferenceRequest;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.order.model.Customer;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.ShippingAddress;

/** Monta a preferência do Checkout Pro sem chamar o Mercado Pago. */
class MercadoPagoGatewayTests {

	private static Order sampleOrder(BigDecimal shipping) {
		Product product = new Product();
		product.setSlug("corrida-dos-sapos");
		product.setName("Corrida dos Sapos");
		product.setPrice(new BigDecimal("89.90"));
		Order order = new Order(new Customer("Maria Souza", "maria@example.com", "11987654321"),
				new ShippingAddress("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"));
		order.addItem(product, 2);
		order.setShippingCost(shipping);
		return order;
	}

	@Test
	void mapsOrderToPreference() {
		MercadoPagoGateway gateway = new MercadoPagoGateway("TEST-123", "https://asobi.com.br/",
				"https://api.asobi.com.br");
		Order order = sampleOrder(BigDecimal.ZERO);

		PreferenceRequest request = gateway.buildPreferenceRequest(order);

		assertThat(request.getExternalReference()).isEqualTo(order.getPublicId().toString());
		assertThat(request.getItems()).singleElement().satisfies(item -> {
			assertThat(item.getTitle()).isEqualTo("Corrida dos Sapos");
			assertThat(item.getQuantity()).isEqualTo(2);
			assertThat(item.getUnitPrice()).isEqualByComparingTo("89.90");
			assertThat(item.getCurrencyId()).isEqualTo("BRL");
		});
		assertThat(request.getPayer().getEmail()).isEqualTo("maria@example.com");
		assertThat(request.getBackUrls().getSuccess())
				.isEqualTo("https://asobi.com.br/pedido/" + order.getPublicId());
		assertThat(request.getAutoReturn()).isEqualTo("approved");
		assertThat(request.getNotificationUrl()).isEqualTo("https://api.asobi.com.br/api/webhooks/mercadopago");
	}

	@Test
	void addsShippingAsItemWhenCharged() {
		MercadoPagoGateway gateway = new MercadoPagoGateway("TEST-123", "https://asobi.com.br", "https://api");
		PreferenceRequest request = gateway.buildPreferenceRequest(sampleOrder(new BigDecimal("19.90")));
		assertThat(request.getItems()).hasSize(2);
		assertThat(request.getItems().get(1).getTitle()).isEqualTo("Frete");
	}

	@Test
	void skipsAutoReturnAndWebhookOnLocalhost() {
		MercadoPagoGateway gateway = new MercadoPagoGateway("TEST-123", "http://localhost:3000",
				"http://localhost:8080");
		PreferenceRequest request = gateway.buildPreferenceRequest(sampleOrder(BigDecimal.ZERO));
		assertThat(request.getAutoReturn()).isNull();
		assertThat(request.getNotificationUrl()).isNull();
	}

	@Test
	void mapsPaymentStatuses() {
		assertThat(MercadoPagoGateway.outcomeOf("approved")).isEqualTo(PaymentUpdate.Outcome.APPROVED);
		assertThat(MercadoPagoGateway.outcomeOf("pending")).isEqualTo(PaymentUpdate.Outcome.PENDING);
		assertThat(MercadoPagoGateway.outcomeOf("in_process")).isEqualTo(PaymentUpdate.Outcome.PENDING);
		assertThat(MercadoPagoGateway.outcomeOf("rejected")).isEqualTo(PaymentUpdate.Outcome.FAILED);
		assertThat(MercadoPagoGateway.outcomeOf("cancelled")).isEqualTo(PaymentUpdate.Outcome.FAILED);
	}
}
