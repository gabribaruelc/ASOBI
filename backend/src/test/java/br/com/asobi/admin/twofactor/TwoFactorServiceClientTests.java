package br.com.asobi.admin.twofactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClient;

import br.com.asobi.admin.twofactor.TwoFactorGateway.Verification;
import br.com.asobi.common.exception.BusinessException;

class TwoFactorServiceClientTests {

	private static final String BASE = "http://two-factor.test";
	private static final String VERIFICATIONS = BASE + "/api/challenges/abc/verifications";
	private static final String ADMIN = "admin@asobi.com.br";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final TwoFactorServiceClient client = new TwoFactorServiceClient(
			new TwoFactorServiceProperties(BASE, "key-123"), builder);

	private static DefaultResponseCreator problem(HttpStatus status, String json) {
		return withStatus(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(json);
	}

	@Test
	void asksForAnAdminLoginCode() {
		server.expect(requestTo(BASE + "/api/challenges"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Api-Key", "key-123"))
				.andExpect(jsonPath("$.email").value(ADMIN))
				.andExpect(jsonPath("$.purpose").value("admin-login"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"id\": \"abc\", \"email\": \"admin@asobi.com.br\", \"purpose\": \"admin-login\"}"));

		assertThat(client.requestCode(ADMIN)).isEqualTo("abc");
		server.verify();
	}

	@Test
	void showsTheRateLimitMessageButHidesOtherErrors() {
		server.expect(requestTo(BASE + "/api/challenges")).andRespond(problem(HttpStatus.TOO_MANY_REQUESTS,
				"{\"status\": 429, \"detail\": \"Muitos códigos pedidos para este e-mail. Tente de novo mais tarde.\"}"));
		assertThatThrownBy(() -> client.requestCode(ADMIN))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Muitos códigos pedidos para este e-mail. Tente de novo mais tarde.");

		server.reset();
		server.expect(requestTo(BASE + "/api/challenges")).andRespond(problem(HttpStatus.UNAUTHORIZED,
				"{\"status\": 401, \"detail\": \"Chave de API inválida.\"}"));
		assertThatThrownBy(() -> client.requestCode(ADMIN))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Não foi possível falar com o serviço de verificação");
	}

	@Test
	void acceptsTheCodeConfirmedForThisEmailAndPurpose() {
		server.expect(requestTo(VERIFICATIONS))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.code").value("123456"))
				.andRespond(withSuccess(
						"{\"id\": \"abc\", \"email\": \"admin@asobi.com.br\", \"purpose\": \"admin-login\", \"verified\": true}",
						MediaType.APPLICATION_JSON));

		assertThat(client.verify("abc", "123456", " Admin@Asobi.com.br ").status())
				.isEqualTo(Verification.Status.VERIFIED);
	}

	@Test
	void refusesACodeConfirmedForAnotherEmailOrPurpose() {
		server.expect(requestTo(VERIFICATIONS)).andRespond(withSuccess(
				"{\"email\": \"outra@example.com\", \"purpose\": \"admin-login\", \"verified\": true}",
				MediaType.APPLICATION_JSON));
		assertThat(client.verify("abc", "123456", ADMIN).status()).isEqualTo(Verification.Status.EXPIRED);

		server.reset();
		server.expect(requestTo(VERIFICATIONS)).andRespond(withSuccess(
				"{\"email\": \"admin@asobi.com.br\", \"purpose\": \"outra-coisa\", \"verified\": true}",
				MediaType.APPLICATION_JSON));
		assertThat(client.verify("abc", "123456", ADMIN).status()).isEqualTo(Verification.Status.EXPIRED);
	}

	@Test
	void wrongCodeTellsHowManyAttemptsAreLeft() {
		server.expect(requestTo(VERIFICATIONS)).andRespond(problem(HttpStatus.UNPROCESSABLE_ENTITY,
				"{\"status\": 422, \"detail\": \"Código incorreto.\", \"attemptsLeft\": 1}"));
		Verification verification = client.verify("abc", "000000", ADMIN);
		assertThat(verification.status()).isEqualTo(Verification.Status.WRONG_CODE);
		assertThat(verification.message()).isEqualTo("Código incorreto. Você tem mais 1 tentativa.");

		server.reset();
		server.expect(requestTo(VERIFICATIONS)).andRespond(problem(HttpStatus.UNPROCESSABLE_ENTITY,
				"{\"status\": 422, \"detail\": \"Código incorreto.\", \"attemptsLeft\": 0}"));
		assertThat(client.verify("abc", "000000", ADMIN).status()).isEqualTo(Verification.Status.EXPIRED);
	}

	@Test
	void expiredOrUnknownChallengeNeedsANewCode() {
		server.expect(requestTo(VERIFICATIONS)).andRespond(problem(HttpStatus.GONE,
				"{\"status\": 410, \"detail\": \"Este código expirou ou já foi usado. Peça um novo.\"}"));
		assertThat(client.verify("abc", "123456", ADMIN).status()).isEqualTo(Verification.Status.EXPIRED);
	}

	@Test
	void neverVerifiesWhenTheServiceFails() {
		server.expect(requestTo(VERIFICATIONS)).andRespond(withServerError());
		assertThat(client.verify("abc", "123456", ADMIN).status()).isEqualTo(Verification.Status.UNAVAILABLE);
	}

	@Test
	void isDisabledWithoutUrl() {
		assertThat(new TwoFactorServiceClient(new TwoFactorServiceProperties("", null)).isEnabled()).isFalse();
		assertThat(client.isEnabled()).isTrue();
	}
}
