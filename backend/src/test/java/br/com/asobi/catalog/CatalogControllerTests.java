package br.com.asobi.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.TestClockConfig;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.review.model.Review;
import br.com.asobi.review.repository.ReviewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class CatalogControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Test
	void listsCategoriesInOrder() throws Exception {
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].slug", contains("ate-3", "4-6", "7-10", "mais-10")));
	}

	@Test
	void listsAllProductsSortedByName() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(10)))
				.andExpect(jsonPath("$[0].name").value("Alvo Certeiro"));
	}

	@Test
	void filtersByCategory() throws Exception {
		mockMvc.perform(get("/api/products").param("category", "4-6"))
				.andExpect(jsonPath("$[*].slug",
						containsInAnyOrder("corrida-dos-sapos", "alvo-certeiro", "torre-magica")));
	}

	@Test
	void filtersCooperative() throws Exception {
		mockMvc.perform(get("/api/products").param("cooperative", "true"))
				.andExpect(jsonPath("$[*].slug",
						containsInAnyOrder("missao-no-castelo", "ilha-do-tesouro-cooperativa")));
	}

	@Test
	void filtersNewArrivalsByServerClock() throws Exception {
		mockMvc.perform(get("/api/products").param("isNew", "true"))
				.andExpect(jsonPath("$[*].slug", containsInAnyOrder("missao-no-castelo", "detetives-da-escola")))
				.andExpect(jsonPath("$[0].isNew").value(true));
	}

	@Test
	void filtersOnSale() throws Exception {
		mockMvc.perform(get("/api/products").param("onSale", "true"))
				.andExpect(jsonPath("$[*].slug",
						containsInAnyOrder("palavras-magicas", "ilha-do-tesouro-cooperativa", "torre-magica")));
	}

	@Test
	void productDetailHidesStockCountAndShowsPromo() throws Exception {
		mockMvc.perform(get("/api/products/torre-magica"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inStock").value(false))
				.andExpect(jsonPath("$.stock").doesNotExist())
				.andExpect(jsonPath("$.price").value(79.90))
				.andExpect(jsonPath("$.promo.originalPrice").value(99.90))
				.andExpect(jsonPath("$.ageKey").value("4-6"));
	}

	@Test
	void productDetailShowsOnlyApprovedReviews() throws Exception {
		Product product = productRepository.findBySlug("corrida-dos-sapos").orElseThrow();
		reviewRepository.save(new Review(product, "Anônimo", 1, "Comentário ainda não moderado"));

		mockMvc.perform(get("/api/products/corrida-dos-sapos"))
				.andExpect(jsonPath("$.reviews", hasSize(2)))
				.andExpect(jsonPath("$.reviews[*].name", not(hasItem("Anônimo"))));
	}

	@Test
	void unknownProductReturns404ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/products/nao-existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("Produto não encontrado: nao-existe"));
	}
}
