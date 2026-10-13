package br.com.asobi.twofactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.twofactor.challenge.Challenge;
import br.com.asobi.twofactor.challenge.ChallengeRepository;
import br.com.asobi.twofactor.challenge.CodeHasher;
import br.com.asobi.twofactor.config.ApiKeyFilter;
import br.com.asobi.twofactor.email.EmailMessage;
import br.com.asobi.twofactor.email.EmailSender;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChallengeApiTests {

	private static final String EMAIL = "admin@asobi.com.br";
	private static final Pattern CODE = Pattern.compile("\\b\\d{6}\\b");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChallengeRepository challengeRepository;

	@Autowired
	private CodeHasher codeHasher;

	@MockitoBean
	private EmailSender emailSender;

	private static MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder request) {
		return request.header(ApiKeyFilter.HEADER, "test-key").contentType(MediaType.APPLICATION_JSON);
	}

	private MockHttpServletRequestBuilder create(String email) {
		return withKey(post("/api/challenges"))
				.content("{\"email\": \"" + email + "\", \"purpose\": \"admin-login\"}");
	}

	private MockHttpServletRequestBuilder verifyCode(UUID id, String code) {
		return withKey(post("/api/challenges/{id}/verifications", id)).content("{\"code\": \"" + code + "\"}");
	}

	private UUID createChallenge(String email) throws Exception {
		String body = mockMvc.perform(create(email)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	/** O código só existe no e-mail: lê do último que o serviço mandou. */
	private String lastSentCode() {
		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(emailSender, atLeastOnce()).send(captor.capture());
		Matcher matcher = CODE.matcher(captor.getValue().text());
		assertThat(matcher.find()).isTrue();
		return matcher.group();
	}

	private static String wrong(String code) {
		return code.equals("000000") ? "111111" : "000000";
	}

	@Test
	void sendsCodeByEmailAndNeverReturnsIt() throws Exception {
		String body = mockMvc.perform(create("  Admin@Asobi.com.br "))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.purpose").value("admin-login"))
				.andExpect(jsonPath("$.expiresAt").exists())
				.andReturn().getResponse().getContentAsString();

		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(emailSender).send(captor.capture());
		String code = lastSentCode();
		assertThat(captor.getValue().to()).isEqualTo(EMAIL);
		assertThat(captor.getValue().html()).contains(code);
		assertThat(body).doesNotContain(code);

		Challenge stored = challengeRepository.findById(UUID.fromString(JsonPath.read(body, "$.id"))).orElseThrow();
		assertThat(stored.getCodeHash()).hasSize(64).doesNotContain(code);
	}

	@Test
	void acceptsTheCodeOnlyOnce() throws Exception {
		UUID id = createChallenge(EMAIL);
		String code = lastSentCode();

		mockMvc.perform(verifyCode(id, code))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.verified").value(true))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.purpose").value("admin-login"));

		mockMvc.perform(verifyCode(id, code))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.detail").value("Este código expirou ou já foi usado. Peça um novo."));
	}

	@Test
	void wrongCodeSpendsAttemptsUntilTheChallengeIsBlocked() throws Exception {
		UUID id = createChallenge(EMAIL);
		String code = lastSentCode();

		for (int left = 4; left >= 0; left--) {
			mockMvc.perform(verifyCode(id, wrong(code)))
					.andExpect(status().is(422))
					.andExpect(jsonPath("$.detail").value("Código incorreto."))
					.andExpect(jsonPath("$.attemptsLeft").value(left));
		}
		// depois do limite, nem o código certo entra
		mockMvc.perform(verifyCode(id, code)).andExpect(status().isGone());
	}

	@Test
	void refusesExpiredCode() throws Exception {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		challengeRepository.save(new Challenge(id, EMAIL, "admin-login", codeHasher.hash(id, "123456"),
				now.minus(11, ChronoUnit.MINUTES), now.minus(1, ChronoUnit.MINUTES)));

		mockMvc.perform(verifyCode(id, "123456")).andExpect(status().isGone());
	}

	@Test
	void newCodeRetiresThePreviousOne() throws Exception {
		UUID first = createChallenge(EMAIL);
		String firstCode = lastSentCode();
		UUID second = createChallenge(EMAIL);
		String secondCode = lastSentCode();

		mockMvc.perform(verifyCode(first, firstCode)).andExpect(status().isGone());
		mockMvc.perform(verifyCode(second, secondCode)).andExpect(status().isOk());
	}

	@Test
	void limitsHowManyCodesAnEmailCanAsk() throws Exception {
		for (int i = 0; i < 5; i++) {
			createChallenge(EMAIL);
		}
		mockMvc.perform(create(EMAIL))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.detail").value("Muitos códigos pedidos para este e-mail. Tente de novo mais tarde."));
		// o limite é por e-mail
		mockMvc.perform(create("outra@example.com")).andExpect(status().isCreated());
	}

	@Test
	void emailFailureLeavesNoChallengeBehind() throws Exception {
		willThrow(new IllegalStateException("Resend fora do ar")).given(emailSender).send(any());

		mockMvc.perform(create(EMAIL))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.detail").value("Não foi possível enviar o código agora. Tente novamente em instantes."));
		assertThat(challengeRepository.count()).isZero();
	}

	@Test
	void validatesInput() throws Exception {
		mockMvc.perform(create("nao-e-email"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("E-mail inválido."));
		mockMvc.perform(withKey(post("/api/challenges")).content("{\"email\": \"" + EMAIL + "\", \"purpose\": \"Admin Login\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.purpose").value("Finalidade inválida."));
		mockMvc.perform(verifyCode(UUID.randomUUID(), "12ab"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.code").value("O código tem 6 números."));
	}

	@Test
	void unknownChallengeIsNotFound() throws Exception {
		mockMvc.perform(verifyCode(UUID.randomUUID(), "123456")).andExpect(status().isNotFound());
	}

	@Test
	void rejectsRequestsWithoutValidApiKey() throws Exception {
		mockMvc.perform(post("/api/challenges").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\": \"" + EMAIL + "\", \"purpose\": \"admin-login\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/challenges/{id}/verifications", UUID.randomUUID())
				.header(ApiKeyFilter.HEADER, "wrong").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"123456\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthCheckIsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
