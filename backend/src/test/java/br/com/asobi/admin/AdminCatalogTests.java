package br.com.asobi.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.TestClockConfig;
import br.com.asobi.catalog.repository.CategoryRepository;
import br.com.asobi.catalog.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class AdminCatalogTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	private MockHttpServletRequestBuilder adminPost(String url, Object... vars) {
		return post(url, vars).with(user(ADMIN)).with(csrf());
	}

	private MockHttpServletRequestBuilder productForm(String url, Object... vars) {
		Long categoryId = categoryRepository.findBySlug("4-6").orElseThrow().getId();
		return adminPost(url, vars)
				.param("name", "Jogo da Memória Animal")
				.param("icon", "🐶")
				.param("description", "Encontre os pares de bichinhos.")
				.param("skill", "Memória")
				.param("categoryId", categoryId.toString())
				.param("ageLabel", "4+ anos")
				.param("players", "2 a 4 jogadores")
				.param("regularPrice", "49.90")
				.param("stock", "5");
	}

	@Test
	void listsProductsWithBadges() throws Exception {
		mockMvc.perform(get("/admin/produtos").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Corrida dos Sapos")))
				.andExpect(content().string(containsString("Esgotado")))
				.andExpect(content().string(containsString("Novidade · 1 dia")));
	}

	@Test
	void createsProductVisibleInStoreApi() throws Exception {
		mockMvc.perform(productForm("/admin/produtos").param("newUntil", "2026-09-30").param("cooperative", "true"))
				.andExpect(redirectedUrl("/admin/produtos"));

		mockMvc.perform(get("/api/products/jogo-da-memoria-animal"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").value(49.90))
				.andExpect(jsonPath("$.promo").doesNotExist())
				.andExpect(jsonPath("$.cooperative").value(true))
				.andExpect(jsonPath("$.isNew").value(true))
				// "até 30/09" = até o fim do dia 30 no horário de Brasília
				.andExpect(jsonPath("$.newUntil").value("2026-10-01T03:00:00Z"));
	}

	@Test
	void duplicateNameGetsUniqueSlug() throws Exception {
		mockMvc.perform(productForm("/admin/produtos"));
		mockMvc.perform(productForm("/admin/produtos"));
		assertThat(productRepository.existsBySlug("jogo-da-memoria-animal-2")).isTrue();
	}

	@Test
	void rejectsSalePriceNotBelowRegularPrice() throws Exception {
		mockMvc.perform(productForm("/admin/produtos").param("onSale", "true").param("salePrice", "59.90"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("O preço promocional precisa ser menor que o preço normal.")));
		assertThat(productRepository.existsBySlug("jogo-da-memoria-animal")).isFalse();
	}

	@Test
	void showsValidationErrors() throws Exception {
		mockMvc.perform(adminPost("/admin/produtos").param("name", "").param("regularPrice", "-1").param("stock", "-2"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Informe o nome.")))
				.andExpect(content().string(containsString("O preço precisa ser maior que zero.")))
				.andExpect(content().string(containsString("O estoque não pode ser negativo.")));
	}

	@Test
	void onSaleProductKeepsRegularPriceAsOriginal() throws Exception {
		mockMvc.perform(productForm("/admin/produtos").param("onSale", "true").param("salePrice", "39.90"));

		mockMvc.perform(get("/api/products/jogo-da-memoria-animal"))
				.andExpect(jsonPath("$.price").value(39.90))
				.andExpect(jsonPath("$.promo.originalPrice").value(49.90));
	}

	@Test
	void editFormShowsRegularAndSalePrices() throws Exception {
		Long id = productRepository.findBySlug("palavras-magicas").orElseThrow().getId();
		mockMvc.perform(get("/admin/produtos/{id}/editar", id).with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("value=\"94.90\"")))
				.andExpect(content().string(containsString("value=\"74.90\"")));
	}

	@Test
	void endingSaleRestoresRegularPrice() throws Exception {
		Long id = productRepository.findBySlug("palavras-magicas").orElseThrow().getId();
		mockMvc.perform(adminPost("/admin/produtos/{id}/encerrar-promocao", id))
				.andExpect(flash().attributeExists("success"));

		mockMvc.perform(get("/api/products/palavras-magicas"))
				.andExpect(jsonPath("$.price").value(94.90))
				.andExpect(jsonPath("$.promo").doesNotExist());
	}

	@Test
	void updatesStockInline() throws Exception {
		Long id = productRepository.findBySlug("corrida-dos-sapos").orElseThrow().getId();
		mockMvc.perform(adminPost("/admin/produtos/{id}/estoque", id).param("stock", "0"));
		mockMvc.perform(get("/api/products/corrida-dos-sapos")).andExpect(jsonPath("$.inStock").value(false));

		mockMvc.perform(adminPost("/admin/produtos/{id}/estoque", id).param("stock", "-1"))
				.andExpect(flash().attribute("error", "O estoque não pode ser negativo."));
	}

	@Test
	void updatesProductKeepingSlug() throws Exception {
		Long id = productRepository.findBySlug("alvo-certeiro").orElseThrow().getId();
		mockMvc.perform(productForm("/admin/produtos/{id}", id))
				.andExpect(redirectedUrl("/admin/produtos"));
		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.name").value("Jogo da Memória Animal"));
	}

	@Test
	void deletesProduct() throws Exception {
		Long id = productRepository.findBySlug("empilha-bichos").orElseThrow().getId();
		mockMvc.perform(adminPost("/admin/produtos/{id}/excluir", id)).andExpect(redirectedUrl("/admin/produtos"));
		mockMvc.perform(get("/api/products/empilha-bichos")).andExpect(status().isNotFound());
	}

	@Test
	void unknownProductShows404() throws Exception {
		mockMvc.perform(get("/admin/produtos/999999/editar").with(user(ADMIN)))
				.andExpect(status().isNotFound());
	}

	@Test
	void createsCategoryAndBlocksDeletingOneInUse() throws Exception {
		mockMvc.perform(adminPost("/admin/categorias").param("name", "11 a 14 anos").param("position", "5"))
				.andExpect(flash().attributeExists("success"));
		mockMvc.perform(get("/api/categories"))
				.andExpect(jsonPath("$[*].slug", hasItem("11-a-14-anos")));

		Long inUse = categoryRepository.findBySlug("4-6").orElseThrow().getId();
		mockMvc.perform(adminPost("/admin/categorias/{id}/excluir", inUse))
				.andExpect(flash().attribute("error",
						"Não dá para excluir: ainda há produtos nessa faixa etária. Mude-os de faixa antes."));

		Long unused = categoryRepository.findBySlug("11-a-14-anos").orElseThrow().getId();
		mockMvc.perform(adminPost("/admin/categorias/{id}/excluir", unused))
				.andExpect(flash().attributeExists("success"));
		mockMvc.perform(get("/api/categories"))
				.andExpect(jsonPath("$[*].slug", not(hasItem("11-a-14-anos"))));
	}

	@Test
	void editsAboutPage() throws Exception {
		MockHttpServletRequestBuilder request = adminPost("/admin/sobre")
				.param("heroText", "Nova história")
				.param("missionEmoji", "⭐")
				.param("missionTitle", "Nova missão")
				.param("missionText", "Texto da missão");
		for (int i = 0; i < 4; i++) {
			request.param("values[" + i + "].icon", "🎈")
					.param("values[" + i + "].title", "Valor " + (i + 1))
					.param("values[" + i + "].text", "Texto " + (i + 1));
		}
		mockMvc.perform(request).andExpect(redirectedUrl("/admin/sobre"));

		mockMvc.perform(get("/api/content/about"))
				.andExpect(jsonPath("$.heroText").value("Nova história"))
				.andExpect(jsonPath("$.values.length()").value(4))
				.andExpect(jsonPath("$.values[3].title").value("Valor 4"))
				.andExpect(jsonPath("$.missionTitle").value("Nova missão"));
	}

	@Test
	void aboutPageFormShowsSeedContent() throws Exception {
		mockMvc.perform(get("/admin/sobre").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Curadoria cuidadosa")));
	}

	@Test
	void aboutPageRejectsEmptyText() throws Exception {
		mockMvc.perform(adminPost("/admin/sobre").param("heroText", "").param("missionTitle", "x").param("missionText", "y"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Escreva a história da ASOBI.")));
	}
}
