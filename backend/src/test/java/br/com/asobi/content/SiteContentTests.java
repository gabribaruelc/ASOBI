package br.com.asobi.content;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SiteContentTests {

	private static final String ADMIN = "priscila@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void siteInfoStartsWithoutPlaceholders() throws Exception {
		mockMvc.perform(get("/api/content/site-info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactEmail").value("contato@asobi.com.br"))
				.andExpect(jsonPath("$.contactPhone").value(nullValue()))
				.andExpect(jsonPath("$.whatsappUrl").value(nullValue()))
				.andExpect(jsonPath("$.cnpj").value(nullValue()));
	}

	@Test
	void adminUpdatesContactShownInStore() throws Exception {
		mockMvc.perform(post("/admin/contato").with(user(ADMIN)).with(csrf())
						.param("contactEmail", "oi@asobi.com.br")
						.param("contactPhone", "(11) 91234-5678")
						.param("businessHours", "")
						.param("instagramUrl", "https://instagram.com/asobi")
						.param("legalName", "ASOBI Jogos Ltda")
						.param("cnpj", "12345678000190"))
				.andExpect(redirectedUrl("/admin/contato"));

		mockMvc.perform(get("/api/content/site-info"))
				.andExpect(jsonPath("$.contactEmail").value("oi@asobi.com.br"))
				.andExpect(jsonPath("$.contactPhone").value("(11) 91234-5678"))
				.andExpect(jsonPath("$.whatsappUrl").value("https://wa.me/5511912345678"))
				.andExpect(jsonPath("$.businessHours").value(nullValue()))
				.andExpect(jsonPath("$.instagramUrl").value("https://instagram.com/asobi"))
				.andExpect(jsonPath("$.legalName").value("ASOBI Jogos Ltda"))
				.andExpect(jsonPath("$.cnpj").value("12.345.678/0001-90"));
	}

	@Test
	void rejectsInvalidContactData() throws Exception {
		mockMvc.perform(post("/admin/contato").with(user(ADMIN)).with(csrf())
						.param("contactEmail", "nao-e-email")
						.param("instagramUrl", "instagram.com/asobi")
						.param("cnpj", "123"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("E-mail inválido.")))
				.andExpect(content().string(containsString("Cole o link completo")))
				.andExpect(content().string(containsString("CNPJ inválido.")));
	}

	@Test
	void servesSeededPolicies() throws Exception {
		mockMvc.perform(get("/api/content/policies/privacy"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Política de Privacidade"))
				.andExpect(jsonPath("$.body", containsString("# Dados de crianças")));
		mockMvc.perform(get("/api/content/policies/returns")).andExpect(status().isOk());
		mockMvc.perform(get("/api/content/policies/shipping")).andExpect(status().isOk());
		mockMvc.perform(get("/api/content/policies/nao-existe")).andExpect(status().isNotFound());
	}

	@Test
	void adminEditsPolicyText() throws Exception {
		mockMvc.perform(get("/admin/politicas").with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Trocas, Devoluções e Garantia")));

		mockMvc.perform(post("/admin/politicas").with(user(ADMIN)).with(csrf())
						.param("policies[0].slug", "shipping")
						.param("policies[0].title", "Política de Frete")
						.param("policies[0].body", "  # Prazo\r\nEnviamos em 2 dias úteis.  "))
				.andExpect(redirectedUrl("/admin/politicas"));

		mockMvc.perform(get("/api/content/policies/shipping"))
				.andExpect(jsonPath("$.body").value("# Prazo\nEnviamos em 2 dias úteis."));
		mockMvc.perform(get("/api/content/policies/privacy"))
				.andExpect(jsonPath("$.body", containsString("LGPD")));
	}

	@Test
	void policyTextIsRequired() throws Exception {
		mockMvc.perform(post("/admin/politicas").with(user(ADMIN)).with(csrf())
						.param("policies[0].slug", "shipping")
						.param("policies[0].title", "Política de Frete")
						.param("policies[0].body", " "))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Escreva o texto.")));
	}
}
