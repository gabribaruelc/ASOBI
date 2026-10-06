package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderEmailTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	private static final String ADDRESS = """
			"shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
			                    "district": "Bela Vista", "city": "São Paulo", "state": "sp"},
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	// O envio real só acontece depois do commit, que não existe nos testes transacionais;
	// aqui conferimos o que foi entregue ao dispatcher.
	@MockitoBean
	private EmailDispatcher dispatcher;

	private Order placeOrder(String customerName) throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"customer": {"name": "%s", "email": "Maria@Example.com", "phone": "(11) 98765-4321"},
				 %s
				 "items": [{"slug": "corrida-dos-sapos", "quantity": 2}, {"slug": "palavras-magicas", "quantity": 1}]}
				""".formatted(customerName, ADDRESS)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String publicId = JsonPath.read(body, "$.orderId");
		return orderRepository.findByPublicId(UUID.fromString(publicId)).orElseThrow();
	}

	private List<EmailMessage> sentEmails() {
		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(dispatcher, atLeastOnce()).sendAfterCommit(captor.capture());
		List<EmailMessage> messages = List.copyOf(captor.getAllValues());
		clearInvocations(dispatcher);
		return messages;
	}

	@Test
	void placingOrderEmailsTheCustomer() throws Exception {
		Order order = placeOrder("Maria Souza");

		assertThat(sentEmails()).singleElement().satisfies(email -> {
			assertThat(email.to()).containsExactly("maria@example.com");
			assertThat(email.subject()).isEqualTo("Recebemos seu pedido nº " + order.getId() + " — ASOBI");
			assertThat(email.html())
					.contains("Oi, Maria!")
					.contains("2 × Corrida dos Sapos")
					.contains("R$ 179,80")
					.contains("R$ 254,70")
					.contains("A combinar")
					.contains("Av. Paulista, 1000 — Bela Vista, São Paulo/SP · CEP 01310-100")
					.contains("href=\"http://localhost:3000/pedido/" + order.getPublicId() + "\"");
		});
	}

	@Test
	void paymentEmailsCustomerAndAdmins() throws Exception {
		Order order = placeOrder("Maria Souza");
		sentEmails();

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order.getId()).with(user(ADMIN)).with(csrf()));

		List<EmailMessage> emails = sentEmails();
		assertThat(emails).hasSize(2);
		assertThat(emails.get(0).to()).containsExactly("maria@example.com");
		assertThat(emails.get(0).subject()).startsWith("Pagamento confirmado: pedido nº " + order.getId());
		assertThat(emails.get(1).to()).containsExactly(ADMIN);
		assertThat(emails.get(1).subject()).isEqualTo("Novo pedido pago nº " + order.getId() + " — R$ 254,70");
		assertThat(emails.get(1).html())
				.contains("Maria Souza pagou o pedido")
				.contains("Contato: maria@example.com · (11) 98765-4321")
				.contains("href=\"http://localhost:8080/admin/pedidos/" + order.getId() + "\"");

		// Pagar de novo é recusado e não reenvia nada.
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order.getId()).with(user(ADMIN)).with(csrf()));
		verify(dispatcher, never()).sendAfterCommit(any());
	}

	@Test
	void shippingEmailsTrackingCode() throws Exception {
		Order order = placeOrder("Maria Souza");
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order.getId()).with(user(ADMIN)).with(csrf()));
		sentEmails();

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-enviado", order.getId()).param("trackingCode", "AA123456789BR")
				.with(user(ADMIN)).with(csrf()));

		assertThat(sentEmails()).singleElement().satisfies(email -> {
			assertThat(email.to()).containsExactly("maria@example.com");
			assertThat(email.subject()).isEqualTo("Seu pedido nº " + order.getId() + " foi enviado — ASOBI");
			assertThat(email.html()).contains("Código de rastreio: AA123456789BR");
		});
	}

	@Test
	void escapesCustomerDataInHtml() throws Exception {
		placeOrder("<b>Maria</b> Souza");

		assertThat(sentEmails().get(0).html()).contains("&lt;b&gt;Maria&lt;/b&gt;").doesNotContain("<b>Maria</b>");
	}

	@Test
	void rejectedOrderSendsNothing() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"customer": {"name": "Maria Souza", "email": "maria@example.com", "phone": "(11) 98765-4321"},
				 %s
				 "items": [{"slug": "torre-magica", "quantity": 1}]}
				""".formatted(ADDRESS)))
				.andExpect(status().isUnprocessableEntity());

		verify(dispatcher, never()).sendAfterCommit(any());
	}
}
