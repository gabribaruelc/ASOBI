package br.com.asobi.adminauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.adminauth.admin.AdminUser;
import br.com.asobi.adminauth.admin.AdminUserRepository;
import br.com.asobi.adminauth.config.ApiKeyFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminApiTests {

	private static final String ADMIN = "admin@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	private static MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder request) {
		return request.header(ApiKeyFilter.HEADER, "test-key");
	}

	private MockHttpServletRequestBuilder addAdmin(String body) {
		return withKey(post("/api/admins")).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void bootstrapAdminIsListed() throws Exception {
		mockMvc.perform(withKey(get("/api/admins")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email").value(hasItem(ADMIN)));
	}

	@Test
	void rejectsRequestsWithoutValidApiKey() throws Exception {
		mockMvc.perform(get("/api/admins")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/authorizations").param("email", ADMIN).header(ApiKeyFilter.HEADER, "wrong"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthCheckIsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void addsAdminNormalizingEmail() throws Exception {
		mockMvc.perform(addAdmin("""
				{"email": "  Nova@Example.com ", "createdByEmail": "Admin@asobi.com.br"}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("nova@example.com"))
				.andExpect(jsonPath("$.createdByEmail").value(ADMIN));
		assertThat(adminUserRepository.existsByEmail("nova@example.com")).isTrue();
	}

	@Test
	void rejectsDuplicateAndInvalidEmail() throws Exception {
		mockMvc.perform(addAdmin("{\"email\": \"" + ADMIN + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Esse e-mail já é admin."));
		mockMvc.perform(addAdmin("{\"email\": \"nao-e-email\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("E-mail inválido."));
	}

	@Test
	void answersWhetherAnEmailIsAdmin() throws Exception {
		mockMvc.perform(withKey(get("/api/authorizations")).param("email", " Admin@Asobi.com.br "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(ADMIN))
				.andExpect(jsonPath("$.admin").value(true));
		mockMvc.perform(withKey(get("/api/authorizations")).param("email", "intruso@example.com"))
				.andExpect(jsonPath("$.admin").value(false));
	}

	@Test
	void removesAdminButNeverTheRequester() throws Exception {
		AdminUser other = adminUserRepository.save(new AdminUser("outra@example.com", ADMIN));

		mockMvc.perform(withKey(delete("/api/admins/{id}", other.getId())).param("requestedBy", "outra@example.com"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Você não pode remover o seu próprio acesso."));

		mockMvc.perform(withKey(delete("/api/admins/{id}", other.getId())).param("requestedBy", ADMIN))
				.andExpect(status().isNoContent());
		mockMvc.perform(withKey(get("/api/authorizations")).param("email", "outra@example.com"))
				.andExpect(jsonPath("$.admin").value(false));

		mockMvc.perform(withKey(delete("/api/admins/{id}", other.getId())).param("requestedBy", ADMIN))
				.andExpect(status().isNotFound());
	}
}
