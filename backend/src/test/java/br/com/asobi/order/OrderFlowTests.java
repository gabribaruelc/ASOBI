package br.com.asobi.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderFlowTests {

	private static final String ADMIN = "dona@asobi.com.br";

	private static final String CUSTOMER = """
			"customer": {"name": "Maria Souza", "email": "Maria@Example.com", "phone": "(11) 98765-4321"},
			"shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
			                    "district": "Bela Vista", "city": "São Paulo", "state": "sp"},
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	private String placeOrder(String itemsJson) throws Exception {
		String body = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + "\"items\": " + itemsJson + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.orderId");
	}

	private Order order(String publicId) {
		return orderRepository.findByPublicId(UUID.fromString(publicId)).orElseThrow();
	}

	private int stockOf(String slug) {
		return productRepository.findBySlug(slug).orElseThrow().getStock();
	}

	@Test
	void placesOrderWithServerSidePrices() throws Exception {
		mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + """
								"items": [{"slug": "corrida-dos-sapos", "quantity": 2, "price": 0.01},
								          {"slug": "palavras-magicas", "quantity": 1}],
								"total": 1.00}
								"""))
				.andExpect(status().isCreated())
				// 2 × 89,90 + 74,90 (preço promocional do banco), ignorando o que o navegador mandou
				.andExpect(jsonPath("$.total").value(254.70))
				.andExpect(jsonPath("$.checkoutUrl", startsWith("http://localhost:3000/pedido/")));

		Order order = orderRepository.findAllByOrderByCreatedAtDesc().get(0);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.getCustomer().getEmail()).isEqualTo("maria@example.com");
		assertThat(order.getShippingAddress().getPostalCode()).isEqualTo("01310-100");
		assertThat(order.getShippingAddress().getState()).isEqualTo("SP");
		assertThat(stockOf("corrida-dos-sapos")).as("estoque só baixa no pagamento").isEqualTo(18);
	}

	@Test
	void mergesRepeatedItems() throws Exception {
		String id = placeOrder("""
				[{"slug": "alvo-certeiro", "quantity": 1}, {"slug": "alvo-certeiro", "quantity": 2}]
				""");
		assertThat(order(id).getItems()).singleElement().extracting("quantity").isEqualTo(3);
	}

	@Test
	void rejectsOutOfStockProduct() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + "\"items\": [{\"slug\": \"torre-magica\", \"quantity\": 1}]}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("\"Torre Mágica\" esgotou. Remova do carrinho para continuar."));
	}

	@Test
	void rejectsQuantityAboveStock() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + "\"items\": [{\"slug\": \"ilha-do-tesouro-cooperativa\", \"quantity\": 8}]}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail", containsString("Só temos 7 unidade(s)")));
	}

	@Test
	void rejectsUnknownProduct() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + "\"items\": [{\"slug\": \"nao-existe\", \"quantity\": 1}]}"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void validatesCustomerAndAddress() throws Exception {
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customer": {"name": "", "email": "x", "phone": "1"},
								 "shippingAddress": {"postalCode": "123", "street": "", "number": "", "district": "", "city": "", "state": "São Paulo"},
								 "items": []}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors['customer.email']").value("E-mail inválido."))
				.andExpect(jsonPath("$.errors['shippingAddress.postalCode']").value("CEP inválido."))
				.andExpect(jsonPath("$.errors['shippingAddress.state']").value("UF inválida."))
				.andExpect(jsonPath("$.errors.items").value("O carrinho está vazio."));
	}

	@Test
	void publicOrderStatusHidesPersonalData() throws Exception {
		String id = placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]");
		mockMvc.perform(get("/api/orders/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.statusLabel").value("Aguardando pagamento"))
				.andExpect(jsonPath("$.items[0].name").value("Alvo Certeiro"))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("maria@example.com"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Paulista"))));
	}

	@Test
	void unknownOrderIs404() throws Exception {
		mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	void adminMarksPaidDecrementingStockOnce() throws Exception {
		String id = placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 2}]");
		Long orderId = order(id).getId();

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", orderId).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attributeExists("success"));
		assertThat(order(id).getStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(stockOf("alvo-certeiro")).isEqualTo(23);

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", orderId).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attribute("error", "Esse pedido não está aguardando pagamento."));
		assertThat(stockOf("alvo-certeiro")).isEqualTo(23);
	}

	@Test
	void secondPaymentForLastUnitIsFlagged() throws Exception {
		productRepository.findBySlug("empilha-bichos").orElseThrow().setStock(1);
		String first = placeOrder("[{\"slug\": \"empilha-bichos\", \"quantity\": 1}]");
		String second = placeOrder("[{\"slug\": \"empilha-bichos\", \"quantity\": 1}]");

		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order(first).getId()).with(user(ADMIN)).with(csrf()));
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order(second).getId()).with(user(ADMIN)).with(csrf()));

		assertThat(order(first).isStockIssue()).isFalse();
		assertThat(order(second).isStockIssue()).isTrue();
		assertThat(stockOf("empilha-bichos")).isZero();
		mockMvc.perform(get("/admin/pedidos/{id}", order(second).getId()).with(user(ADMIN)))
				.andExpect(content().string(containsString("Estoque insuficiente")));
	}

	@Test
	void shipsAndCancelsFollowingRules() throws Exception {
		String pending = placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]");
		Long pendingId = order(pending).getId();
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-enviado", pendingId).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attribute("error", "Só pedidos pagos podem ser marcados como enviados."));
		mockMvc.perform(post("/admin/pedidos/{id}/cancelar", pendingId).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attributeExists("success"));
		assertThat(order(pending).getStatus()).isEqualTo(OrderStatus.CANCELED);

		String paid = placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]");
		Long paidId = order(paid).getId();
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", paidId).with(user(ADMIN)).with(csrf()));
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-enviado", paidId).param("trackingCode", " AA123456789BR ")
				.with(user(ADMIN)).with(csrf()));
		assertThat(order(paid).getStatus()).isEqualTo(OrderStatus.SHIPPED);
		assertThat(order(paid).getTrackingCode()).isEqualTo("AA123456789BR");

		mockMvc.perform(get("/api/orders/{id}", paid)).andExpect(jsonPath("$.trackingCode").value("AA123456789BR"));
	}

	@Test
	void adminOrderPagesRender() throws Exception {
		String id = placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]");
		mockMvc.perform(get("/admin/pedidos").param("status", "aguardando-pagamento").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Maria Souza")));
		mockMvc.perform(get("/admin/pedidos/{id}", order(id).getId()).with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("https://wa.me/5511987654321")))
				.andExpect(content().string(containsString("CEP 01310-100")));
		mockMvc.perform(get("/admin").with(user(ADMIN)))
				.andExpect(content().string(containsString("Aguardando pagamento")));
	}
}
