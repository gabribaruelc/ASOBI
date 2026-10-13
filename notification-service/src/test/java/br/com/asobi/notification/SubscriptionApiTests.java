package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.notification.config.ApiKeyFilter;
import br.com.asobi.notification.subscription.AlertType;
import br.com.asobi.notification.subscription.Subscription;
import br.com.asobi.notification.subscription.SubscriptionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionApiTests {

	private static final String EMAIL = "maria@example.com";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	private static MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder request) {
		return request.header(ApiKeyFilter.HEADER, "test-key");
	}

	private MockHttpServletRequestBuilder subscribe(String body) {
		return withKey(post("/api/subscriptions")).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private Subscription saved(String email, long productId, AlertType type) {
		return subscriptionRepository.save(new Subscription(email, productId, type,
				"token-" + email + "-" + productId + "-" + type, Instant.now()));
	}

	@Test
	void subscribesWithConsentNormalizingEmail() throws Exception {
		mockMvc.perform(subscribe("""
				{"email": "  Maria@Example.com ", "productId": 7, "type": "PROMOTION", "consent": true}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.productId").value(7))
				.andExpect(jsonPath("$.type").value("PROMOTION"))
				.andExpect(jsonPath("$.consentedAt").exists())
				.andExpect(jsonPath("$.unsubscribeToken").doesNotExist());

		Subscription stored = subscriptionRepository.findByEmailAndProductIdAndType(EMAIL, 7L, AlertType.PROMOTION)
				.orElseThrow();
		assertThat(stored.getUnsubscribeToken()).hasSizeGreaterThanOrEqualTo(40);
	}

	@Test
	void refusesSubscriptionWithoutConsent() throws Exception {
		mockMvc.perform(subscribe("""
				{"email": "maria@example.com", "productId": 7, "type": "PROMOTION", "consent": false}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.consent").value("É preciso autorizar o envio do aviso."));
		mockMvc.perform(subscribe("""
				{"email": "maria@example.com", "productId": 7, "type": "PROMOTION"}
				"""))
				.andExpect(status().isBadRequest());
		assertThat(subscriptionRepository.count()).isZero();
	}

	@Test
	void validatesInput() throws Exception {
		mockMvc.perform(subscribe("""
				{"email": "nao-e-email", "type": "PROMOTION", "consent": true}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("E-mail inválido."))
				.andExpect(jsonPath("$.errors.productId").value("Informe o produto."));
		mockMvc.perform(subscribe("""
				{"email": "maria@example.com", "productId": 7, "type": "PRICE_DROP", "consent": true}
				"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void askingAgainReturnsTheSameSubscription() throws Exception {
		String body = """
				{"email": "maria@example.com", "productId": 7, "type": "BACK_IN_STOCK", "consent": true}
				""";
		mockMvc.perform(subscribe(body)).andExpect(status().isCreated());
		mockMvc.perform(subscribe(body)).andExpect(status().isOk());
		assertThat(subscriptionRepository.count()).isEqualTo(1);

		// outro tipo de aviso para o mesmo produto é outra inscrição
		mockMvc.perform(subscribe(body.replace("BACK_IN_STOCK", "PROMOTION"))).andExpect(status().isCreated());
		assertThat(subscriptionRepository.count()).isEqualTo(2);
	}

	@Test
	void listsByEmailAndByProduct() throws Exception {
		saved(EMAIL, 7, AlertType.PROMOTION);
		saved(EMAIL, 8, AlertType.BACK_IN_STOCK);
		saved("joao@example.com", 7, AlertType.PROMOTION);

		mockMvc.perform(withKey(get("/api/subscriptions")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
		mockMvc.perform(withKey(get("/api/subscriptions")).param("email", " Maria@example.com"))
				.andExpect(jsonPath("$", hasSize(2)));
		mockMvc.perform(withKey(get("/api/subscriptions")).param("productId", "7"))
				.andExpect(jsonPath("$", hasSize(2)));
		mockMvc.perform(withKey(get("/api/subscriptions")).param("email", EMAIL).param("productId", "8"))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].type").value("BACK_IN_STOCK"));
	}

	@Test
	void removesOneSubscription() throws Exception {
		Subscription subscription = saved(EMAIL, 7, AlertType.PROMOTION);

		mockMvc.perform(withKey(delete("/api/subscriptions/{id}", subscription.getId())))
				.andExpect(status().isNoContent());
		mockMvc.perform(withKey(delete("/api/subscriptions/{id}", subscription.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void removesEverythingFromAnEmailOrAProduct() throws Exception {
		saved(EMAIL, 7, AlertType.PROMOTION);
		saved(EMAIL, 8, AlertType.BACK_IN_STOCK);
		saved("joao@example.com", 7, AlertType.PROMOTION);
		saved("joao@example.com", 9, AlertType.PROMOTION);

		mockMvc.perform(withKey(delete("/api/subscriptions")).param("email", "MARIA@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.removed").value(2));
		mockMvc.perform(withKey(delete("/api/subscriptions")).param("productId", "7"))
				.andExpect(jsonPath("$.removed").value(1));
		assertThat(subscriptionRepository.count()).isEqualTo(1);

		// sem filtro (ou com os dois) não apaga nada
		mockMvc.perform(withKey(delete("/api/subscriptions"))).andExpect(status().isBadRequest());
		mockMvc.perform(withKey(delete("/api/subscriptions")).param("email", EMAIL).param("productId", "9"))
				.andExpect(status().isBadRequest());
		assertThat(subscriptionRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsRequestsWithoutValidApiKey() throws Exception {
		mockMvc.perform(get("/api/subscriptions")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/subscriptions").header(ApiKeyFilter.HEADER, "wrong"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/product-events").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthCheckIsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
