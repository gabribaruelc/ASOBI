package br.com.asobi.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.review.model.Review;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewFlowTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReviewRepository reviewRepository;

	private Review submit(String json) throws Exception {
		mockMvc.perform(post("/api/products/alvo-certeiro/reviews")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("pending"));
		return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING).get(0);
	}

	@Test
	void submittedReviewIsPendingAndHiddenFromStore() throws Exception {
		Review review = submit("""
				{"name": " Ana ", "rating": 5, "comment": "Muito divertido!", "status": "APPROVED"}
				""");
		assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
		assertThat(review.getAuthorName()).isEqualTo("Ana");

		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.reviews[*].name", not(hasItem("Ana"))));
		mockMvc.perform(get("/admin/avaliacoes").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Muito divertido!")));
	}

	@Test
	void approvedReviewAppearsInStoreAndRejectedDisappears() throws Exception {
		Review review = submit("""
				{"name": "Ana", "rating": 4, "comment": "Legal"}
				""");

		mockMvc.perform(post("/admin/avaliacoes/{id}/aprovar", review.getId()).with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/avaliacoes?status=pendentes"));
		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.reviews[*].name", hasItem("Ana")));

		mockMvc.perform(post("/admin/avaliacoes/{id}/rejeitar", review.getId()).param("tab", "aprovadas")
						.with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/avaliacoes?status=aprovadas"));
		mockMvc.perform(get("/api/products/alvo-certeiro"))
				.andExpect(jsonPath("$.reviews[*].name", not(hasItem("Ana"))));
	}

	@Test
	void deletesReview() throws Exception {
		Review review = submit("""
				{"name": "Spam", "rating": 1, "comment": "compre aqui"}
				""");
		mockMvc.perform(post("/admin/avaliacoes/{id}/excluir", review.getId()).with(user(ADMIN)).with(csrf()));
		assertThat(reviewRepository.findById(review.getId())).isEmpty();
	}

	@Test
	void validatesReview() throws Exception {
		mockMvc.perform(post("/api/products/alvo-certeiro/reviews")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "", "rating": 6, "comment": ""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").value("Informe seu nome."))
				.andExpect(jsonPath("$.errors.rating").value("A nota vai de 1 a 5."))
				.andExpect(jsonPath("$.errors.comment").value("Escreva um comentário."));
	}

	@Test
	void reviewForUnknownProductIs404() throws Exception {
		mockMvc.perform(post("/api/products/nao-existe/reviews")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Ana", "rating": 5, "comment": "Oi"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void storeCanPostFromAnotherOrigin() throws Exception {
		mockMvc.perform(options("/api/products/alvo-certeiro/reviews")
						.header("Origin", "http://localhost:3000")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
	}

	@Test
	void moderationRequiresAdmin() throws Exception {
		mockMvc.perform(post("/admin/avaliacoes/1/aprovar").with(csrf()))
				.andExpect(redirectedUrl("/admin/login"));
	}
}
