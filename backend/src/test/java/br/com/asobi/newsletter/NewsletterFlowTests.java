package br.com.asobi.newsletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.newsletter.model.NewsletterSubscriber;
import br.com.asobi.newsletter.repository.NewsletterSubscriberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NewsletterFlowTests {

	private static final String ADMIN = "admin@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NewsletterSubscriberRepository subscriberRepository;

	private ResultActions subscribe(String email) throws Exception {
		return mockMvc.perform(post("/api/newsletter-subscriptions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + email + "\"}"));
	}

	@Test
	void subscribesAndShowsInAdmin() throws Exception {
		subscribe("Ana@Example.com").andExpect(status().isNoContent());

		assertThat(subscriberRepository.findAll()).extracting(NewsletterSubscriber::getEmail)
				.containsExactly("ana@example.com");
		mockMvc.perform(get("/admin/newsletter").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("ana@example.com")));
	}

	@Test
	void subscribingTwiceKeepsOneRow() throws Exception {
		subscribe("ana@example.com").andExpect(status().isNoContent());
		subscribe("ANA@example.com").andExpect(status().isNoContent());

		assertThat(subscriberRepository.count()).isEqualTo(1);
	}

	@Test
	void validatesEmail() throws Exception {
		subscribe("nao-e-email")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("E-mail inválido."));
		assertThat(subscriberRepository.count()).isZero();
	}

	@Test
	void exportsCsv() throws Exception {
		subscribe("ana@example.com");

		mockMvc.perform(get("/admin/newsletter/exportar").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", containsString("newsletter-asobi.csv")))
				.andExpect(content().string(containsString("email,inscrito_em\nana@example.com,")));
	}

	@Test
	void adminRemovesSubscriber() throws Exception {
		subscribe("ana@example.com");
		Long id = subscriberRepository.findAll().get(0).getId();

		mockMvc.perform(post("/admin/newsletter/{id}/excluir", id).with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/newsletter"));
		assertThat(subscriberRepository.count()).isZero();
	}

	@Test
	void listRequiresAdmin() throws Exception {
		mockMvc.perform(get("/admin/newsletter/exportar"))
				.andExpect(redirectedUrl("/admin/login"));
	}
}
