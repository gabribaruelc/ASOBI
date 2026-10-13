package br.com.asobi.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.alert.ProductAlertGateway.Alert;
import br.com.asobi.catalog.event.ProductEvent;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class ProductAlertTests {

	private static final String ADMIN = "admin@asobi.com.br";
	// no catálogo de exemplo: em estoque e fora de promoção
	private static final String SLUG = "corrida-dos-sapos";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductAlertListener listener;

	@Autowired
	private ApplicationEvents events;

	@MockitoBean
	private ProductAlertGateway gateway;

	@BeforeEach
	void alertsAreOn() {
		given(gateway.isEnabled()).willReturn(true);
	}

	private Product product() {
		return productRepository.findBySlug(SLUG).orElseThrow();
	}

	private ResultActions askAlert(String slug, String type, boolean consent) throws Exception {
		return mockMvc.perform(post("/api/product-alerts").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "Maria@Example.com", "productSlug": "%s", "type": "%s", "consent": %s}
				""".formatted(slug, type, consent)));
	}

	private MockHttpServletRequestBuilder adminPost(String url, Object... vars) {
		return post(url, vars).with(user(ADMIN)).with(csrf());
	}

	private List<ProductEvent> productEvents() {
		return events.stream(ProductEvent.class).toList();
	}

	@Test
	void storeAsksToBeToldAboutAPromotion() throws Exception {
		askAlert(SLUG, "PROMOTION", true).andExpect(status().isNoContent());

		verify(gateway).subscribe("Maria@Example.com", product().getId(), AlertType.PROMOTION);
	}

	@Test
	void refusesWithoutConsent() throws Exception {
		askAlert(SLUG, "PROMOTION", false)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.consent").value("Marque a caixa para autorizar o envio do aviso."));

		verify(gateway, never()).subscribe(any(), any(), any());
	}

	@Test
	void onlyAcceptsAlertsThatStillMakeSense() throws Exception {
		askAlert(SLUG, "BACK_IN_STOCK", true)
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value("Este jogo já está disponível."));

		product().setStock(0);
		askAlert(SLUG, "BACK_IN_STOCK", true).andExpect(status().isNoContent());

		product().setOriginalPrice(new BigDecimal("199.90"));
		askAlert(SLUG, "PROMOTION", true)
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value("Este jogo já está em promoção."));

		askAlert("nao-existe", "PROMOTION", true).andExpect(status().isNotFound());
	}

	@Test
	void serviceFailureBecomesFriendlyError() throws Exception {
		willThrow(new BusinessException("Não foi possível registrar o aviso agora. Tente novamente em instantes."))
				.given(gateway).subscribe(any(), any(), any());

		askAlert(SLUG, "PROMOTION", true)
				.andExpect(status().is(422))
				.andExpect(jsonPath("$.detail").value("Não foi possível registrar o aviso agora. Tente novamente em instantes."));
	}

	@Test
	void settingsTellTheStoreWhetherAlertsAreOn() throws Exception {
		mockMvc.perform(get("/api/settings")).andExpect(jsonPath("$.productAlertsEnabled").value(true));

		given(gateway.isEnabled()).willReturn(false);
		mockMvc.perform(get("/api/settings")).andExpect(jsonPath("$.productAlertsEnabled").value(false));
	}

	@Test
	void restockingPublishesBackInStockOnlyWhenItWasSoldOut() throws Exception {
		Long id = product().getId();

		mockMvc.perform(adminPost("/admin/produtos/{id}/estoque", id).param("stock", "9"));
		assertThat(productEvents()).isEmpty();

		mockMvc.perform(adminPost("/admin/produtos/{id}/estoque", id).param("stock", "0"));
		assertThat(productEvents()).isEmpty();

		mockMvc.perform(adminPost("/admin/produtos/{id}/estoque", id).param("stock", "3"));
		assertThat(productEvents()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(ProductEvent.Type.BACK_IN_STOCK);
			assertThat(event.productId()).isEqualTo(id);
			assertThat(event.slug()).isEqualTo(SLUG);
		});
	}

	@Test
	void puttingOnSalePublishesSaleStartedOnce() throws Exception {
		Product product = product();
		MockHttpServletRequestBuilder form = adminPost("/admin/produtos/{id}", product.getId())
				.param("name", product.getName())
				.param("description", product.getDescription())
				.param("skill", product.getSkill())
				.param("categoryId", product.getCategory().getId().toString())
				.param("ageLabel", product.getAgeLabel())
				.param("players", product.getPlayers())
				.param("regularPrice", "89.90")
				.param("onSale", "true")
				.param("salePrice", "69.90")
				.param("stock", "5");

		mockMvc.perform(form).andExpect(status().is3xxRedirection());
		assertThat(productEvents()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo(ProductEvent.Type.SALE_STARTED);
			assertThat(event.price()).isEqualByComparingTo("69.90");
			assertThat(event.originalPrice()).isEqualByComparingTo("89.90");
		});

		// salvar de novo, ainda em promoção, não avisa outra vez
		mockMvc.perform(form.param("salePrice", "59.90"));
		assertThat(productEvents()).hasSize(1);
	}

	@Test
	void deletingAProductPublishesDeleted() throws Exception {
		Long id = product().getId();

		mockMvc.perform(adminPost("/admin/produtos/{id}/excluir", id));

		assertThat(productEvents()).extracting(ProductEvent::type).containsExactly(ProductEvent.Type.DELETED);
	}

	@Test
	void listenerTranslatesCatalogEventsForTheService() {
		listener.onProductEvent(new ProductEvent(ProductEvent.Type.SALE_STARTED, 7L, SLUG, "Corrida dos Sapos",
				new BigDecimal("69.90"), new BigDecimal("89.90")));

		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(gateway).publish(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new Alert(AlertType.PROMOTION, 7L, "Corrida dos Sapos",
				"http://localhost:3000/jogos/corrida-dos-sapos", new BigDecimal("69.90"), new BigDecimal("89.90")));

		listener.onProductEvent(new ProductEvent(ProductEvent.Type.DELETED, 7L, SLUG, "Corrida dos Sapos",
				new BigDecimal("69.90"), null));
		verify(gateway).removeProduct(7L);
	}

	@Test
	void listenerNeverBreaksThePanelWhenTheServiceIsDown() {
		willThrow(new IllegalStateException("fora do ar")).given(gateway).publish(any());

		listener.onProductEvent(new ProductEvent(ProductEvent.Type.BACK_IN_STOCK, 7L, SLUG, "Corrida dos Sapos",
				new BigDecimal("89.90"), null));

		verify(gateway).publish(any());
	}
}
