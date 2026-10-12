package br.com.asobi.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import br.com.asobi.admin.service.AdminDashboardService;
import br.com.asobi.admin.service.AdminDashboardService.DashboardMetrics;
import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private AdminDashboardService dashboardService;

	private Long placeOrder(String itemsJson) throws Exception {
		String body = mockMvc.perform(post("/api/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customer": {"name": "Maria Souza", "email": "maria@example.com", "phone": "(11) 98765-4321"},
								 "shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
								                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
								 "items": %s}
								""".formatted(itemsJson)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String publicId = JsonPath.read(body, "$.orderId");
		return orderRepository.findByPublicId(UUID.fromString(publicId)).orElseThrow().getId();
	}

	private void markPaid(Long orderId) throws Exception {
		mockMvc.perform(post("/admin/pedidos/{id}/marcar-pago", orderId).with(user(ADMIN)).with(csrf()));
	}

	@Test
	void revenueCountsOnlyPaidOrders() throws Exception {
		markPaid(placeOrder("[{\"slug\": \"corrida-dos-sapos\", \"quantity\": 2}]"));
		placeOrder("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]");

		DashboardMetrics metrics = dashboardService.metrics(30);

		assertThat(metrics.revenue()).isEqualByComparingTo("179.80");
		assertThat(metrics.paidOrders()).isEqualTo(1);
		assertThat(metrics.placedOrders()).isEqualTo(2);
		assertThat(metrics.averageTicket()).isEqualByComparingTo("179.80");
		assertThat(metrics.daily()).hasSize(30);
		assertThat(metrics.daily().get(29).revenue()).as("hoje é a última barra").isEqualByComparingTo("179.80");
		assertThat(metrics.daily().get(29).percent()).isEqualTo(100);
		assertThat(metrics.bestSellers()).singleElement().satisfies(item -> {
			assertThat(item.name()).isEqualTo("Corrida dos Sapos");
			assertThat(item.quantity()).isEqualTo(2);
			assertThat(item.revenue()).isEqualByComparingTo(new BigDecimal("179.80"));
		});
	}

	@Test
	void countsProductViewsPerProduct() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/products/{slug}/views", "corrida-dos-sapos")).andExpect(status().isNoContent());
		}
		mockMvc.perform(post("/api/products/{slug}/views", "alvo-certeiro")).andExpect(status().isNoContent());
		mockMvc.perform(post("/api/products/{slug}/views", "nao-existe")).andExpect(status().isNotFound());

		DashboardMetrics metrics = dashboardService.metrics(7);

		assertThat(metrics.views()).isEqualTo(4);
		assertThat(metrics.mostViewed()).extracting("slug", "views")
				.containsExactly(org.assertj.core.groups.Tuple.tuple("corrida-dos-sapos", 3L),
						org.assertj.core.groups.Tuple.tuple("alvo-certeiro", 1L));
	}

	@Test
	void dashboardShowsMetricsForTheChosenPeriod() throws Exception {
		markPaid(placeOrder("[{\"slug\": \"corrida-dos-sapos\", \"quantity\": 2}]"));

		mockMvc.perform(get("/admin").param("periodo", "7").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("R$ 179,80")))
				.andExpect(content().string(containsString("Mais vendidos")))
				.andExpect(content().string(containsString("Corrida dos Sapos")));
	}

	@Test
	void emptyPeriodAndUnknownPeriodStillRender() throws Exception {
		mockMvc.perform(get("/admin").param("periodo", "999").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nenhum pedido pago nos últimos <span>30</span> dias.")));
	}
}
