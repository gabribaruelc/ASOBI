package br.com.asobi.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.admin.twofactor.TwoFactorGateway;
import br.com.asobi.admin.twofactor.TwoFactorGateway.Verification;
import br.com.asobi.common.exception.BusinessException;
import jakarta.servlet.http.Cookie;

/** Painel com o two-factor-service ligado: depois do login, só entra quem digitar o código. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminTwoFactorTests {

	private static final String ADMIN = "admin@asobi.com.br";
	private static final String PAGE = "/admin/verificacao";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TwoFactorGateway gateway;

	// A sessão do painel fica no banco (Spring Session): o que liga uma requisição à outra é o cookie.
	private Cookie sessionCookie;

	@BeforeEach
	void twoFactorIsOn() {
		given(gateway.isEnabled()).willReturn(true);
		given(gateway.requestCode(ADMIN)).willReturn("challenge-1");
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

	private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
		return as(ADMIN, request);
	}

	private ResultActions typeCode(String code) throws Exception {
		return asAdmin(post(PAGE).param("code", code).with(csrf()));
	}

	@Test
	void loggedAdminWithoutCodeIsSentToVerification() throws Exception {
		asAdmin(get("/admin")).andExpect(redirectedUrl(PAGE));
		asAdmin(get("/admin/pedidos")).andExpect(redirectedUrl(PAGE));
	}

	@Test
	void verificationPageSendsTheCodeOnlyOnce() throws Exception {
		asAdmin(get(PAGE))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Enviamos um código de 6 números")))
				.andExpect(content().string(containsString(ADMIN)));
		asAdmin(get(PAGE)).andExpect(status().isOk());

		verify(gateway, times(1)).requestCode(ADMIN);
	}

	@Test
	void rightCodeOpensThePanel() throws Exception {
		given(gateway.verify("challenge-1", "123456", ADMIN))
				.willReturn(new Verification(Verification.Status.VERIFIED, null));
		asAdmin(get(PAGE));

		typeCode(" 123456 ").andExpect(redirectedUrl("/admin"));

		asAdmin(get("/admin")).andExpect(status().isOk());
		// quem já passou não fica preso na tela do código
		asAdmin(get(PAGE)).andExpect(redirectedUrl("/admin"));
	}

	@Test
	void verifiedSessionDoesNotOpenThePanelForAnotherAccount() throws Exception {
		given(gateway.verify("challenge-1", "123456", ADMIN))
				.willReturn(new Verification(Verification.Status.VERIFIED, null));
		asAdmin(get(PAGE));
		typeCode("123456").andExpect(redirectedUrl("/admin"));

		as("intruso@example.com", get("/admin")).andExpect(redirectedUrl("/admin/login?denied"));
	}

	@Test
	void wrongCodeKeepsThePanelClosed() throws Exception {
		given(gateway.verify("challenge-1", "000000", ADMIN)).willReturn(
				new Verification(Verification.Status.WRONG_CODE, "Código incorreto. Você tem mais 4 tentativas."));
		asAdmin(get(PAGE));

		typeCode("000000")
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("error", "Código incorreto. Você tem mais 4 tentativas."));

		asAdmin(get("/admin")).andExpect(redirectedUrl(PAGE));
		// o mesmo código continua valendo: nada de reenviar sozinho
		asAdmin(get(PAGE));
		verify(gateway, times(1)).requestCode(ADMIN);
	}

	@Test
	void malformedCodeIsNotSentToTheService() throws Exception {
		asAdmin(get(PAGE));

		typeCode("12ab")
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("error", "O código tem 6 números."));

		verify(gateway, never()).verify(any(), any(), any());
	}

	@Test
	void expiredCodeIsReplacedWhenAsked() throws Exception {
		given(gateway.verify("challenge-1", "123456", ADMIN)).willReturn(
				new Verification(Verification.Status.EXPIRED, "Este código expirou ou já foi usado. Peça um novo."));
		asAdmin(get(PAGE));
		typeCode("123456")
				.andExpect(flash().attribute("error", "Este código expirou ou já foi usado. Peça um novo."));

		given(gateway.requestCode(ADMIN)).willReturn("challenge-2");
		asAdmin(post(PAGE + "/reenviar").with(csrf()))
				.andExpect(redirectedUrl(PAGE))
				.andExpect(flash().attribute("success", "Enviamos um código novo."));

		given(gateway.verify("challenge-2", "654321", ADMIN))
				.willReturn(new Verification(Verification.Status.VERIFIED, null));
		typeCode("654321").andExpect(redirectedUrl("/admin"));
	}

	@Test
	void sendFailureShowsTheMessageAndKeepsThePanelClosed() throws Exception {
		given(gateway.requestCode(ADMIN))
				.willThrow(new BusinessException("Muitos códigos pedidos para este e-mail. Tente de novo mais tarde."));

		asAdmin(get(PAGE))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Muitos códigos pedidos para este e-mail.")))
				.andExpect(content().string(containsString("Enviar código")));
		asAdmin(get("/admin")).andExpect(redirectedUrl(PAGE));
	}

	@Test
	void verificationIsOnlyForLoggedAdmins() throws Exception {
		mockMvc.perform(get(PAGE)).andExpect(redirectedUrl("/admin/login"));
		mockMvc.perform(get(PAGE).with(user("intruso@example.com")))
				.andExpect(redirectedUrl("/admin/login?denied"));
		verify(gateway, never()).requestCode(any());
	}

	@Test
	void storeApiAndLoginPageStayOpen() throws Exception {
		mockMvc.perform(get("/api/products")).andExpect(status().isOk());
		mockMvc.perform(get("/admin/login")).andExpect(status().isOk());
	}
}
