package br.com.asobi.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShippingFlowTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	private static final String CUSTOMER = """
			"customer": {"name": "Maria", "email": "maria@example.com", "phone": "11987654321"},
			"shippingAddress": {"postalCode": "20040-002", "street": "Rua X", "number": "1",
			                    "district": "Centro", "city": "Rio de Janeiro", "state": "RJ"},
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@MockitoBean
	private ShippingQuoteProvider provider;

	@BeforeEach
	void stubProvider() {
		given(provider.isConfigured()).willReturn(true);
		given(provider.quote(any())).willReturn(List.of(
				new ShippingOption("1", "PAC", "Correios", new BigDecimal("25.50"), null, 6),
				new ShippingOption("2", "SEDEX", "Correios", new BigDecimal("40.00"), null, 2)));
	}

	private MockHttpServletRequestBuilder saveSettings(boolean enabled, String originPostalCode) {
		return post("/admin/configuracoes").with(user(ADMIN)).with(csrf())
				.param("shippingEnabled", String.valueOf(enabled))
				.param("originPostalCode", originPostalCode)
				.param("freeShippingThreshold", "150.00")
				.param("packageWeightKg", "1.0")
				.param("packageWidthCm", "30")
				.param("packageHeightCm", "8")
				.param("packageLengthCm", "30");
	}

	private void enableShipping() throws Exception {
		mockMvc.perform(saveSettings(true, "01310100")).andExpect(redirectedUrl("/admin/configuracoes"));
	}

	private MockHttpServletRequestBuilder quote(String itemsJson) {
		return post("/api/shipping/quote").contentType(MediaType.APPLICATION_JSON)
				.content("{\"postalCode\": \"20040-002\", \"items\": " + itemsJson + "}");
	}

	@Test
	void shippingStartsDisabled() throws Exception {
		mockMvc.perform(get("/api/settings"))
				.andExpect(jsonPath("$.shippingEnabled").value(false))
				.andExpect(jsonPath("$.freeShippingThreshold").value(150.00));
		mockMvc.perform(quote("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void enablingRequiresOriginPostalCode() throws Exception {
		mockMvc.perform(saveSettings(true, ""))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Informe o CEP de origem")));
	}

	@Test
	void enablingRequiresMelhorEnvioToken() throws Exception {
		given(provider.isConfigured()).willReturn(false);
		mockMvc.perform(saveSettings(true, "01310-100"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("MELHOR_ENVIO_TOKEN")));
	}

	@Test
	void quotesShippingWhenEnabled() throws Exception {
		enableShipping();
		mockMvc.perform(get("/api/settings")).andExpect(jsonPath("$.shippingEnabled").value(true));
		mockMvc.perform(quote("[{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("1"))
				.andExpect(jsonPath("$[0].price").value(25.50))
				.andExpect(jsonPath("$[0].originalPrice").doesNotExist());
	}

	@Test
	void cheapestOptionIsFreeAboveThreshold() throws Exception {
		enableShipping();
		// 2 × 89,90 = 179,80 ≥ 150,00
		mockMvc.perform(quote("[{\"slug\": \"corrida-dos-sapos\", \"quantity\": 2}]"))
				.andExpect(jsonPath("$[0].price").value(0))
				.andExpect(jsonPath("$[0].originalPrice").value(25.50))
				.andExpect(jsonPath("$[1].price").value(40.00));
	}

	@Test
	void orderRequiresShippingOptionWhenEnabled() throws Exception {
		enableShipping();
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + "\"items\": [{\"slug\": \"alvo-certeiro\", \"quantity\": 1}]}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Escolha uma opção de frete."));
	}

	@Test
	void orderChargesServerQuotedShipping() throws Exception {
		enableShipping();
		String body = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + """
								"items": [{"slug": "alvo-certeiro", "quantity": 1}],
								"shippingOptionId": "2", "shippingCost": 0.01}
								"""))
				.andExpect(status().isCreated())
				// 64,90 + 40,00 de SEDEX (cotado de novo no servidor)
				.andExpect(jsonPath("$.total").value(104.90))
				.andReturn().getResponse().getContentAsString();

		Order order = orderRepository.findByPublicId(UUID.fromString(JsonPath.read(body, "$.orderId"))).orElseThrow();
		assertThat(order.getShippingCost()).isEqualByComparingTo("40.00");
		assertThat(order.getShippingService()).isEqualTo("Correios SEDEX · 2 dias úteis");
	}

	@Test
	void rejectsUnavailableShippingOption() throws Exception {
		enableShipping();
		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
						.content("{" + CUSTOMER + """
								"items": [{"slug": "alvo-certeiro", "quantity": 1}], "shippingOptionId": "99"}
								"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(
						"A opção de frete escolhida não está mais disponível. Escolha o frete de novo."));
	}

	@Test
	void settingsPageRenders() throws Exception {
		mockMvc.perform(get("/admin/configuracoes").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Calcular frete automaticamente")));
	}
}
