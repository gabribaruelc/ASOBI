package br.com.asobi.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.notification.EmailDispatcher;
import br.com.asobi.notification.EmailMessage;
import br.com.asobi.order.controller.PaymentReminderJobController;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.repository.OrderRepository;
import br.com.asobi.payment.PaymentGateway;

@SpringBootTest(properties = "asobi.jobs.api-key=job-key")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentReminderTests {

	private static final String ADMIN = "admin@asobi.com.br";
	private static final String CHECKOUT_URL = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// o envio real só acontece depois do commit, que não existe nos testes transacionais
	@MockitoBean
	private EmailDispatcher dispatcher;

	// os lembretes só valem com Mercado Pago; no modo manual quem confirma é o lojista
	@MockitoBean
	private PaymentGateway paymentGateway;

	@BeforeEach
	void mercadoPago() {
		given(paymentGateway.name()).willReturn("mercadopago");
		given(paymentGateway.createCheckout(any())).willReturn(CHECKOUT_URL);
	}

	private Order placeOrder() throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"customer": {"name": "Maria Souza", "email": "maria@example.com", "phone": "(11) 98765-4321"},
				 "shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
				                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
				 "items": [{"slug": "corrida-dos-sapos", "quantity": 2}]}
				""")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		clearInvocations(dispatcher);
		return orderRepository.findByPublicId(UUID.fromString(JsonPath.read(body, "$.orderId"))).orElseThrow();
	}

	private void createdHoursAgo(Order order, int hours) {
		orderRepository.flush();
		jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
				OffsetDateTime.now(ZoneOffset.UTC).minusHours(hours), order.getId());
	}

	private ResultActions runJob(String key) throws Exception {
		return mockMvc.perform(post("/api/jobs/payment-reminders").header(PaymentReminderJobController.HEADER, key));
	}

	private List<EmailMessage> sentEmails() {
		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(dispatcher, atLeastOnce()).sendAfterCommit(captor.capture());
		return captor.getAllValues();
	}

	@Test
	void remindsOnceAboutAnUnpaidOrder() throws Exception {
		Order order = placeOrder();
		createdHoursAgo(order, 3);

		runJob("job-key").andExpect(status().isOk()).andExpect(jsonPath("$.sent").value(1));

		assertThat(sentEmails()).singleElement().satisfies(email -> {
			assertThat(email.to()).containsExactly("maria@example.com");
			assertThat(email.subject()).isEqualTo("Seu pedido nº " + order.getId() + " está esperando pagamento — ASOBI");
			assertThat(email.html())
					.contains("Pagar agora")
					.contains("href=\"http://localhost:3000/pedido/" + order.getPublicId() + "\"");
		});

		runJob("job-key").andExpect(jsonPath("$.sent").value(0));
	}

	@Test
	void waitsAndThenGivesUp() throws Exception {
		Order recent = placeOrder();
		createdHoursAgo(recent, 1);
		Order old = placeOrder();
		createdHoursAgo(old, 72);

		runJob("job-key").andExpect(jsonPath("$.sent").value(0));
	}

	@Test
	void skipsOrdersThatStartedPaymentOrCannotBePaid() throws Exception {
		Order pixGenerated = placeOrder();
		pixGenerated.recordPayment("mercadopago", "123", "pending", "pix");
		createdHoursAgo(pixGenerated, 3);

		Order soldOut = placeOrder();
		createdHoursAgo(soldOut, 3);
		productRepository.findBySlug("corrida-dos-sapos").orElseThrow().setStock(1);

		runJob("job-key").andExpect(jsonPath("$.sent").value(0));
	}

	@Test
	void requiresTheJobKey() throws Exception {
		runJob("errada").andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/jobs/payment-reminders")).andExpect(status().isUnauthorized());
	}

	@Test
	void orderPageCanResumeThePayment() throws Exception {
		Order order = placeOrder();

		mockMvc.perform(get("/api/orders/{id}", order.getPublicId())).andExpect(jsonPath("$.payable").value(true));
		mockMvc.perform(post("/api/orders/{id}/checkouts", order.getPublicId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.checkoutUrl").value(CHECKOUT_URL));

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order.getId()).with(user(ADMIN)).with(csrf()));
		mockMvc.perform(get("/api/orders/{id}", order.getPublicId())).andExpect(jsonPath("$.payable").value(false));
		mockMvc.perform(post("/api/orders/{id}/checkouts", order.getPublicId()))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value("Este pedido não está aguardando pagamento."));
	}

	@Test
	void resumingChecksTheStockAgain() throws Exception {
		Order order = placeOrder();
		productRepository.findBySlug("corrida-dos-sapos").orElseThrow().setStock(1);

		mockMvc.perform(post("/api/orders/{id}/checkouts", order.getPublicId()))
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value(
						"\"Corrida dos Sapos\" esgotou. Monte o carrinho de novo para fazer um novo pedido."));
	}
}
