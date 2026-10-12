package br.com.asobi.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.TestClockConfig;
import br.com.asobi.catalog.dto.ProductImageView;
import br.com.asobi.catalog.repository.CategoryRepository;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.catalog.service.ProductImageService;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class ProductImageTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	// Só o começo do arquivo importa: é por ele que o tipo é reconhecido.
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0 };

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductImageService imageService;

	@Autowired
	private EntityManager entityManager;

	private static MockMultipartFile photo(String filename, byte[] content) {
		return new MockMultipartFile("photos", filename, "application/octet-stream", content);
	}

	private MockMultipartHttpServletRequestBuilder productForm(String url, Object... vars) {
		Long categoryId = categoryRepository.findBySlug("4-6").orElseThrow().getId();
		MockMultipartHttpServletRequestBuilder request = multipart(url, vars);
		request.with(user(ADMIN)).with(csrf())
				.param("name", "Jogo da Memória Animal")
				.param("description", "Encontre os pares de bichinhos.")
				.param("skill", "Memória")
				.param("categoryId", categoryId.toString())
				.param("ageLabel", "4+ anos")
				.param("players", "2 a 4 jogadores")
				.param("regularPrice", "49.90")
				.param("stock", "5");
		return request;
	}

	private Long productId(String slug) {
		return productRepository.findBySlug(slug).orElseThrow().getId();
	}

	private List<ProductImageView> imagesOf(Long productId) {
		entityManager.flush();
		return imageService.list(productId);
	}

	@Test
	void productWithoutPhotosHasEmptyImages() throws Exception {
		mockMvc.perform(get("/api/products/corrida-dos-sapos"))
				.andExpect(jsonPath("$.images").isArray())
				.andExpect(jsonPath("$.images").isEmpty());
	}

	@Test
	void createsProductWithPhotosServedFromStorage() throws Exception {
		mockMvc.perform(productForm("/admin/produtos").file(photo("capa.png", PNG)).file(photo("caixa.jpeg", JPEG)))
				.andExpect(redirectedUrl("/admin/produtos"));

		mockMvc.perform(get("/api/products/jogo-da-memoria-animal"))
				.andExpect(jsonPath("$.images.length()").value(2))
				.andExpect(jsonPath("$.images[0]").value(containsString("/uploads/products/")))
				.andExpect(jsonPath("$.images[0]").value(endsWith(".png")))
				.andExpect(jsonPath("$.images[1]").value(endsWith(".jpg")));

		String url = imagesOf(productId("jogo-da-memoria-animal")).get(0).url();
		mockMvc.perform(get(URI.create(url).getPath()))
				.andExpect(status().isOk())
				.andExpect(content().bytes(PNG));
	}

	@Test
	void ignoresEmptyFileInput() throws Exception {
		// Sem foto escolhida, o navegador manda o campo com um arquivo vazio.
		mockMvc.perform(productForm("/admin/produtos").file(photo("", new byte[0])))
				.andExpect(redirectedUrl("/admin/produtos"));
		assertThat(imagesOf(productId("jogo-da-memoria-animal"))).isEmpty();
	}

	@Test
	void rejectsFileThatIsNotAnImage() throws Exception {
		byte[] script = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
		mockMvc.perform(productForm("/admin/produtos").file(photo("foto.png", script)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("não é uma foto válida")));
		assertThat(productRepository.existsBySlug("jogo-da-memoria-animal")).isFalse();
	}

	@Test
	void rejectsPhotoAboveSizeLimit() throws Exception {
		byte[] huge = new byte[(int) ProductImageService.MAX_BYTES + 1];
		System.arraycopy(PNG, 0, huge, 0, PNG.length);
		mockMvc.perform(productForm("/admin/produtos").file(photo("enorme.png", huge)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("grande demais")));
	}

	@Test
	void rejectsMoreThanMaxPhotos() throws Exception {
		MockMultipartHttpServletRequestBuilder request = productForm("/admin/produtos");
		for (int i = 0; i <= ProductImageService.MAX_IMAGES; i++) {
			request.file(photo("foto" + i + ".png", PNG));
		}
		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Cada produto pode ter até 6 fotos.")));
	}

	@Test
	void editingAddsPhotosAndShowsThemInTheForm() throws Exception {
		Long id = productId("alvo-certeiro");
		mockMvc.perform(productForm("/admin/produtos/{id}", id).file(photo("a.png", PNG)));
		mockMvc.perform(productForm("/admin/produtos/{id}", id).file(photo("b.jpg", JPEG)));
		assertThat(imagesOf(id)).hasSize(2);

		mockMvc.perform(get("/admin/produtos/{id}/editar", id).with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Tornar capa")))
				.andExpect(content().string(containsString("/fotos/" + imagesOf(id).get(1).id() + "/excluir")));

		mockMvc.perform(get("/admin/produtos").with(user(ADMIN)))
				.andExpect(content().string(containsString(imagesOf(id).get(0).url())));
	}

	@Test
	void makesCoverAndDeletesPhoto() throws Exception {
		Long id = productId("alvo-certeiro");
		mockMvc.perform(productForm("/admin/produtos/{id}", id).file(photo("a.png", PNG)).file(photo("b.jpg", JPEG)));
		List<ProductImageView> images = imagesOf(id);

		mockMvc.perform(post("/admin/produtos/{id}/fotos/{imageId}/capa", id, images.get(1).id())
				.with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/produtos/" + id + "/editar"));
		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.images[0]").value(images.get(1).url()))
				.andExpect(jsonPath("$.images[1]").value(images.get(0).url()));

		mockMvc.perform(post("/admin/produtos/{id}/fotos/{imageId}/excluir", id, images.get(1).id())
				.with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/produtos/" + id + "/editar"));
		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.images.length()").value(1))
				.andExpect(jsonPath("$.images[0]").value(images.get(0).url()));
		mockMvc.perform(get(URI.create(images.get(1).url()).getPath())).andExpect(status().isNotFound());
	}

	@Test
	void photoOfAnotherProductIsNotFound() throws Exception {
		Long id = productId("alvo-certeiro");
		mockMvc.perform(productForm("/admin/produtos/{id}", id).file(photo("a.png", PNG)));
		Long imageId = imagesOf(id).get(0).id();

		mockMvc.perform(post("/admin/produtos/{id}/fotos/{imageId}/excluir", productId("corrida-dos-sapos"), imageId)
				.with(user(ADMIN)).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void photoUploadRequiresAdmin() throws Exception {
		mockMvc.perform(multipart("/admin/produtos").file(photo("a.png", PNG)).with(csrf()))
				.andExpect(status().is3xxRedirection());
		assertThat(productRepository.existsBySlug("jogo-da-memoria-animal")).isFalse();
	}
}
