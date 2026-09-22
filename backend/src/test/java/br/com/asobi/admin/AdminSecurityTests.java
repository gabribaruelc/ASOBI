package br.com.asobi.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.admin.model.AdminUser;
import br.com.asobi.admin.repository.AdminUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityTests {

	private static final String ADMIN = "dona@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Test
	void anonymousIsRedirectedToLogin() throws Exception {
		mockMvc.perform(get("/admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login"));
	}

	@Test
	void loginPageIsPublic() throws Exception {
		mockMvc.perform(get("/admin/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Modo desenvolvimento")));
	}

	@Test
	void adminSeesDashboard() throws Exception {
		mockMvc.perform(get("/admin").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Avaliações aguardando")));
	}

	@Test
	void googleLoginWithAdminEmailSeesDashboard() throws Exception {
		mockMvc.perform(get("/admin").with(oidcLogin().idToken(token -> token.claim("email", ADMIN))))
				.andExpect(status().isOk());
	}

	@Test
	void loggedInNonAdminIsDenied() throws Exception {
		mockMvc.perform(get("/admin").with(user("intruso@example.com")))
				.andExpect(redirectedUrl("/admin/login?denied"));
	}

	@Test
	void devLoginWithAdminEmailOpensPanel() throws Exception {
		mockMvc.perform(post("/admin/dev-login").param("email", " Dona@Asobi.com.br ").with(csrf()))
				.andExpect(redirectedUrl("/admin"));
	}

	@Test
	void devLoginWithUnknownEmailFails() throws Exception {
		mockMvc.perform(post("/admin/dev-login").param("email", "intruso@example.com").with(csrf()))
				.andExpect(redirectedUrl("/admin/login?error"));
	}

	@Test
	void adminFormsRequireCsrf() throws Exception {
		mockMvc.perform(post("/admin/admins").param("email", "nova@example.com").with(user(ADMIN)))
				.andExpect(status().is3xxRedirection());
		org.assertj.core.api.Assertions.assertThat(adminUserRepository.existsByEmail("nova@example.com")).isFalse();
	}

	@Test
	void addsAdminNormalizingEmail() throws Exception {
		mockMvc.perform(post("/admin/admins").param("email", "  Nova@Example.com").with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/admins"))
				.andExpect(flash().attributeExists("success"));
		org.assertj.core.api.Assertions.assertThat(adminUserRepository.existsByEmail("nova@example.com")).isTrue();
	}

	@Test
	void rejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/admin/admins").param("email", "nao-e-email").with(user(ADMIN)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("E-mail inválido.")));
	}

	@Test
	void cannotRemoveYourself() throws Exception {
		Long ownId = adminUserRepository.findAll().stream()
				.filter(admin -> admin.getEmail().equals(ADMIN)).findFirst().orElseThrow().getId();
		mockMvc.perform(post("/admin/admins/{id}/excluir", ownId).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attribute("error", "Você não pode remover o seu próprio acesso."));
	}

	@Test
	void removedAdminLosesAccessImmediately() throws Exception {
		AdminUser other = adminUserRepository.save(new AdminUser("outra@example.com", ADMIN));
		mockMvc.perform(post("/admin/admins/{id}/excluir", other.getId()).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attributeExists("success"));

		mockMvc.perform(get("/admin").with(user("outra@example.com")))
				.andExpect(redirectedUrl("/admin/login?denied"));
		mockMvc.perform(get("/admin/admins").with(user(ADMIN)))
				.andExpect(content().string(not(containsString("outra@example.com"))));
	}

	@Test
	void publicApiStaysOpen() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk());
	}
}
