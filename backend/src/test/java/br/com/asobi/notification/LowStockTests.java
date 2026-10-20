package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.alert.AlertType;
import br.com.asobi.alert.ProductAlertGateway;
import br.com.asobi.alert.ProductAlertGateway.Alert;
import br.com.asobi.alert.ProductAlertListener;
import br.com.asobi.catalog.event.ProductEvent;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.repository.OrderRepository;
import br.com.asobi.settings.service.StoreSettingsService;

/** Estoque baixo: aviso para os admins, "últimas unidades" na loja e o aviso de "está acabando". */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class LowStockTests {

	private static final String ADMIN = "admin@asobi.com.br";
	private static final String SLUG = "corrida-dos-sapos";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private StoreSettingsService settingsService;

	@Autowired
	private ProductAlertListener alertListener;

	@Autowired
	private ApplicationEvents events;

	// o envio real só acontece depois do commit, que não existe nos testes transacionais
	@MockitoBean
	private EmailDispatcher dispatcher;

	@MockitoBean
	private ProductAlertGateway gateway;

	@BeforeEach
	void alertsAreOn() {
		given(gateway.isEnabled()).willReturn(true);
	}

	private Product product() {
		return productRepository.findBySlug(SLUG).orElseThrow();
	}

	/** Pedido de {@code quantity} unidades, pago pelo painel. */
	private void sell(int quantity) throws Exception {
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"customer": {"name": "Maria Souza", "email": "maria@example.com", "phone": "(11) 98765-4321"},
				 "shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
				                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
				 "items": [{"slug": "%s", "quantity": %d}]}
				""".formatted(SLUG, quantity)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Order order = orderRepository.findByPublicId(UUID.fromString(JsonPath.read(body, "$.orderId")))
				.orElseThrow();
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", order.getId()).with(user(ADMIN)).with(csrf()));
	}

	private List<ProductEvent> lowStockEvents() {
		return events.stream(ProductEvent.class).filter(event -> event.type() == ProductEvent.Type.LOW_STOCK).toList();
	}

	private List<EmailMessage> adminEmails() {
		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(dispatcher, atLeastOnce()).sendAfterCommit(captor.capture());
		return captor.getAllValues().stream().filter(email -> email.to().contains(ADMIN)
				&& email.subject().matches("(Estoque baixo|Esgotou).*")).toList();
	}

	private ResultActions askAlert(String type) throws Exception {
		return mockMvc.perform(post("/api/product-alerts").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "maria@example.com", "productSlug": "%s", "type": "%s", "consent": true}
				""".formatted(SLUG, type)));
	}

	@Test
	void saleThatCrossesTheThresholdWarnsTheAdmins() throws Exception {
		product().setStock(5);

		sell(2);

		assertThat(lowStockEvents()).singleElement().satisfies(event -> assertThat(event.stock()).isEqualTo(3));
		assertThat(adminEmails()).singleElement().satisfies(email -> {
			assertThat(email.subject()).isEqualTo("Estoque baixo: Corrida dos Sapos (3 unidades)");
			assertThat(email.html())
					.contains("Restam 3 unidades")
					.contains("href=\"http://localhost:8080/admin/produtos/" + product().getId() + "/editar\"");
		});
	}

	@Test
	void sellingTheLastUnitsSaysSoldOut() throws Exception {
		product().setStock(4);

		sell(4);

		assertThat(adminEmails()).singleElement()
				.satisfies(email -> assertThat(email.subject()).isEqualTo("Esgotou: Corrida dos Sapos"));
	}

	@Test
	void warnsOnlyOnceWhenItCrosses() throws Exception {
		product().setStock(10);
		sell(2);
		assertThat(lowStockEvents()).isEmpty();

		product().setStock(3);
		sell(1);
		assertThat(lowStockEvents()).isEmpty();
	}

	@Test
	void thresholdZeroTurnsItOff() throws Exception {
		settingsService.get().setLowStockThreshold(0);
		product().setStock(5);

		sell(4);

		assertThat(lowStockEvents()).isEmpty();
	}

	@Test
	void listenerTellsTheServiceOnlyWhileThereIsStock() {
		alertListener.onProductEvent(new ProductEvent(ProductEvent.Type.LOW_STOCK, 7L, SLUG, "Corrida dos Sapos",
				new BigDecimal("89.90"), null, 2));
		alertListener.onProductEvent(new ProductEvent(ProductEvent.Type.LOW_STOCK, 7L, SLUG, "Corrida dos Sapos",
				new BigDecimal("89.90"), null, 0));

		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(gateway).publish(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new Alert(AlertType.LOW_STOCK, 7L, "Corrida dos Sapos",
				"http://localhost:3000/jogos/corrida-dos-sapos", new BigDecimal("89.90"), null, 2));
	}

	@Test
	void storeShowsLastUnitsAndOnlyAcceptsTheAlertBeforeThat() throws Exception {
		product().setStock(10);
		mockMvc.perform(get("/api/products/{slug}", SLUG)).andExpect(jsonPath("$.lowStock").value(false));
		askAlert("LOW_STOCK").andExpect(status().isNoContent());
		verify(gateway).subscribe("maria@example.com", product().getId(), AlertType.LOW_STOCK);

		product().setStock(2);
		mockMvc.perform(get("/api/products/{slug}", SLUG)).andExpect(jsonPath("$.lowStock").value(true));
		askAlert("LOW_STOCK")
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value("Este jogo já está nas últimas unidades: garanta o seu!"));

		product().setStock(0);
		mockMvc.perform(get("/api/products/{slug}", SLUG)).andExpect(jsonPath("$.lowStock").value(false));
		askAlert("LOW_STOCK").andExpect(status().is(422));
	}

	@Test
	void dashboardListsLowStock() throws Exception {
		product().setStock(2);

		mockMvc.perform(get("/admin").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Estoque baixo (até 3 unidades)")))
				.andExpect(content().string(containsString("Repor estoque")));
	}
}
