package br.com.asobi.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest(properties = "asobi.mercado-pago.webhook-secret=segredo-de-teste")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentWebhookTests {

	private static final String SECRET = "segredo-de-teste";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@MockitoBean
	private PaymentGateway paymentGateway;

	@BeforeEach
	void stubCheckout() {
		given(paymentGateway.name()).willReturn("mercadopago");
		given(paymentGateway.createCheckout(any())).willReturn("https://www.mercadopago.com.br/checkout/abc");
	}

	private UUID placeOrder() throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
						{"customer": {"name": "Maria", "email": "maria@example.com", "phone": "11987654321"},
						 "shippingAddress": {"postalCode": "01310-100", "street": "Av. Paulista", "number": "1000",
						                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
						 "items": [{"slug": "alvo-certeiro", "quantity": 2}]}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.checkoutUrl").value("https://www.mercadopago.com.br/checkout/abc"))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.orderId"));
	}

	private MockHttpServletRequestBuilder signedWebhook(String paymentId) {
		String ts = "1704908010";
		String v1 = new MercadoPagoSignatureVerifier(SECRET)
				.hmacSha256("id:" + paymentId + ";request-id:req-1;ts:" + ts + ";");
		return post("/api/webhooks/mercadopago")
				.param("type", "payment")
				.param("data.id", paymentId)
				.header("x-request-id", "req-1")
				.header("x-signature", "ts=" + ts + ",v1=" + v1)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\": \"payment\", \"data\": {\"id\": \"" + paymentId + "\"}}");
	}

	private Order order(UUID id) {
		return orderRepository.findByPublicId(id).orElseThrow();
	}

	private int stock() {
		return productRepository.findBySlug("alvo-certeiro").orElseThrow().getStock();
	}

	@Test
	void approvedPaymentMarksOrderPaidOnlyOnce() throws Exception {
		UUID orderId = placeOrder();
		given(paymentGateway.fetchPayment("555")).willReturn(Optional.of(
				new PaymentUpdate("555", orderId, "approved", "pix", PaymentUpdate.Outcome.APPROVED)));

		mockMvc.perform(signedWebhook("555")).andExpect(status().isOk());
		mockMvc.perform(signedWebhook("555")).andExpect(status().isOk());

		Order order = order(orderId);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(order.getPaymentId()).isEqualTo("555");
		assertThat(order.getPaymentMethod()).isEqualTo("pix");
		assertThat(stock()).as("webhook repetido não baixa o estoque de novo").isEqualTo(23);
	}

	@Test
	void pendingPixKeepsOrderWaiting() throws Exception {
		UUID orderId = placeOrder();
		given(paymentGateway.fetchPayment("556")).willReturn(Optional.of(
				new PaymentUpdate("556", orderId, "pending", "pix", PaymentUpdate.Outcome.PENDING)));
		mockMvc.perform(signedWebhook("556")).andExpect(status().isOk());
		assertThat(order(orderId).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order(orderId).getPaymentStatus()).isEqualTo("pending");
	}

	@Test
	void rejectedPaymentCancelsOrder() throws Exception {
		UUID orderId = placeOrder();
		given(paymentGateway.fetchPayment("557")).willReturn(Optional.of(
				new PaymentUpdate("557", orderId, "rejected", "visa", PaymentUpdate.Outcome.FAILED)));
		mockMvc.perform(signedWebhook("557")).andExpect(status().isOk());
		assertThat(order(orderId).getStatus()).isEqualTo(OrderStatus.CANCELED);
		assertThat(stock()).isEqualTo(25);
	}

	@Test
	void rejectsInvalidSignature() throws Exception {
		mockMvc.perform(post("/api/webhooks/mercadopago")
						.param("type", "payment").param("data.id", "555")
						.header("x-request-id", "req-1")
						.header("x-signature", "ts=1,v1=deadbeef"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void ignoresOtherNotificationTypes() throws Exception {
		String v1 = new MercadoPagoSignatureVerifier(SECRET).hmacSha256("id:99;request-id:req-1;ts:1;");
		mockMvc.perform(post("/api/webhooks/mercadopago")
						.param("type", "merchant_order").param("data.id", "99")
						.header("x-request-id", "req-1")
						.header("x-signature", "ts=1,v1=" + v1))
				.andExpect(status().isOk());
	}

	@Test
	void paymentForUnknownOrderIsAcknowledged() throws Exception {
		given(paymentGateway.fetchPayment("558")).willReturn(Optional.of(
				new PaymentUpdate("558", UUID.randomUUID(), "approved", "pix", PaymentUpdate.Outcome.APPROVED)));
		mockMvc.perform(signedWebhook("558")).andExpect(status().isOk());
	}

	@Test
	void returnFromCheckoutSyncsPayment() throws Exception {
		UUID orderId = placeOrder();
		given(paymentGateway.fetchPayment("559")).willReturn(Optional.of(
				new PaymentUpdate("559", orderId, "approved", "master", PaymentUpdate.Outcome.APPROVED)));

		mockMvc.perform(post("/api/orders/{id}/payment-sync", orderId)
						.contentType(MediaType.APPLICATION_JSON).content("{\"paymentId\": \"559\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	@Test
	void syncRejectsPaymentOfAnotherOrder() throws Exception {
		UUID orderId = placeOrder();
		given(paymentGateway.fetchPayment("560")).willReturn(Optional.of(
				new PaymentUpdate("560", UUID.randomUUID(), "approved", "pix", PaymentUpdate.Outcome.APPROVED)));

		mockMvc.perform(post("/api/orders/{id}/payment-sync", orderId)
						.contentType(MediaType.APPLICATION_JSON).content("{\"paymentId\": \"560\"}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Esse pagamento não pertence a este pedido."));
		assertThat(order(orderId).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	/** Sem a transação do teste, para ver o rollback real do placeOrder. */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void checkoutFailureReturns502AndKeepsNoOrder() throws Exception {
		given(paymentGateway.createCheckout(any())).willThrow(
				new PaymentException("Não foi possível iniciar o pagamento. Tente novamente em instantes.", null));
		long before = orderRepository.count();

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
						{"customer": {"name": "Maria", "email": "maria@example.com", "phone": "11987654321"},
						 "shippingAddress": {"postalCode": "01310-100", "street": "Av. Paulista", "number": "1000",
						                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
						 "items": [{"slug": "alvo-certeiro", "quantity": 1}]}
						"""))
				.andExpect(status().isBadGateway());
		assertThat(orderRepository.count()).isEqualTo(before);
	}
}
