package br.com.asobi.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.admin.directory.AdminAccount;
import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.admin.twofactor.TwoFactorGateway;
import jakarta.servlet.http.Cookie;

/** A conta principal confirma o login com senha; as outras continuam recebendo o código. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext
class AdminPasswordTests {

	private static final String MAIN = "admin@asobi.com.br";
	private static final String OTHER = "outra@asobi.com.br";
	private static final String PASSWORD = "senha-de-teste";
	private static final String PAGE = "/admin/verificacao";

	@DynamicPropertySource
	static void password(DynamicPropertyRegistry registry) {
		registry.add("asobi.admin-password.hash", () -> BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4)));
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminDirectory adminDirectory;

	@MockitoBean
	private TwoFactorGateway gateway;

	private Cookie sessionCookie;

	@BeforeEach
	void setUp() {
		given(gateway.isEnabled()).willReturn(true);
		given(gateway.requestCode(OTHER)).willReturn("challenge-1");
		if (adminDirectory.listAdmins().stream().map(AdminAccount::email).noneMatch(OTHER::equals)) {
			adminDirectory.addAdmin(OTHER, MAIN);
		}
	}

	private ResultActions as(String email, MockHttpServletRequestBuilder request) throws Exception {
		if (sessionCookie != null) {
			request.cookie(sessionCookie);
		}
		ResultActions result = mockMvc.perform(request.with(user(email)));
		Cookie issued = result.andReturn().getResponse().getCookie("SESSION");
		if (issued != null) {
			sessionCookie = issued;
		}
		return result;
	}

	private ResultActions typePassword(String password) throws Exception {
		return as(MAIN, post(PAGE + "/senha").param("password", password).with(csrf()));
	}

	@Test
	void mainAccountIsAskedForThePasswordAndNoEmailIsSent() throws Exception {
		as(MAIN, get("/admin")).andExpect(redirectedUrl(PAGE));
		as(MAIN, get(PAGE))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Digite a senha do painel")))
				.andExpect(content().string(not(containsString("Enviar código"))));
		as(MAIN, post(PAGE + "/reenviar").with(csrf())).andExpect(redirectedUrl(PAGE));

		verify(gateway, never()).requestCode(any());
	}

	@Test
	void rightPasswordOpensThePanel() throws Exception {
		typePassword(PASSWORD).andExpect(redirectedUrl("/admin"));
		as(MAIN, get("/admin")).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordKeepsThePanelClosed() throws Exception {
		typePassword("errada")
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("error", "Senha incorreta."));
		as(MAIN, get("/admin")).andExpect(redirectedUrl(PAGE));
	}

	@Test
	void otherAdminsStillGetTheCodeAndCannotUseThePassword() throws Exception {
		as(OTHER, get(PAGE))
				.andExpect(content().string(containsString("Enviamos um código de 6 números")));
		verify(gateway).requestCode(OTHER);

		as(OTHER, post(PAGE + "/senha").param("password", PASSWORD).with(csrf()))
				.andExpect(redirectedUrl(PAGE));
		as(OTHER, get("/admin")).andExpect(redirectedUrl(PAGE));
	}
}
