package br.com.asobi.adminauth;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.asobi.adminauth.config.ApiKeyFilter;
import br.com.asobi.adminauth.login.GoogleTokenVerifier;
import br.com.asobi.adminauth.login.GoogleTokenVerifier.GoogleIdentity;
import br.com.asobi.adminauth.login.InvalidTokenException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginApiTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleTokenVerifier tokenVerifier;

	private MockHttpServletRequestBuilder login(String idToken) {
		return post("/api/logins").header(ApiKeyFilter.HEADER, "test-key")
				.contentType(MediaType.APPLICATION_JSON).content("{\"idToken\": \"" + idToken + "\"}");
	}

	@Test
	void acceptsVerifiedAdminEmail() throws Exception {
		given(tokenVerifier.verify("good")).willReturn(new GoogleIdentity("Admin@asobi.com.br", true));
		mockMvc.perform(login("good"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@asobi.com.br"))
				.andExpect(jsonPath("$.admin").value(true));
	}

	@Test
	void refusesEmailOutsideTheList() throws Exception {
		given(tokenVerifier.verify("stranger")).willReturn(new GoogleIdentity("intruso@example.com", true));
		mockMvc.perform(login("stranger"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Este e-mail não tem acesso ao painel."));
	}

	@Test
	void refusesUnverifiedEmail() throws Exception {
		given(tokenVerifier.verify("unverified")).willReturn(new GoogleIdentity("admin@asobi.com.br", false));
		mockMvc.perform(login("unverified")).andExpect(status().isForbidden());
	}

	@Test
	void refusesInvalidToken() throws Exception {
		given(tokenVerifier.verify("forged")).willThrow(new InvalidTokenException("Token do Google inválido.", null));
		mockMvc.perform(login("forged")).andExpect(status().isUnauthorized());
	}

	@Test
	void requiresToken() throws Exception {
		mockMvc.perform(login(""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.idToken").value("Informe o ID token do Google."));
	}
}
