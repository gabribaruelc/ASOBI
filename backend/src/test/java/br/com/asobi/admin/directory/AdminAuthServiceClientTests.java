package br.com.asobi.admin.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;

class AdminAuthServiceClientTests {

	private static final String BASE = "http://admin-auth.test";
	private static final String ADMIN = "admin@asobi.com.br";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final AdminAuthServiceClient client = new AdminAuthServiceClient(
			new AdminAuthServiceProperties(BASE, "key-123"), builder);

	private static String problem(String detail) {
		return "{\"status\": 409, \"detail\": \"" + detail + "\"}";
	}

	@Test
	void asksTheServiceAndCachesTheAnswer() {
		server.expect(once(), requestTo(BASE + "/api/authorizations?email=admin@asobi.com.br"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Api-Key", "key-123"))
				.andRespond(withSuccess("{\"email\": \"admin@asobi.com.br\", \"admin\": true}",
						MediaType.APPLICATION_JSON));

		assertThat(client.isAdmin(" Admin@Asobi.com.br ")).isTrue();
		assertThat(client.isAdmin(ADMIN)).as("segunda pergunta vem do cache").isTrue();
		server.verify();
	}

	@Test
	void deniesWhenServiceIsDownOrEmailIsBlank() {
		server.expect(requestTo(BASE + "/api/authorizations?email=admin@asobi.com.br"))
				.andRespond(withServiceUnavailable());

		assertThat(client.isAdmin(ADMIN)).isFalse();
		assertThat(client.isAdmin(null)).isFalse();
		assertThat(client.isAdmin("  ")).isFalse();
	}

	@Test
	void removingAnAdminClearsTheCache() {
		server.expect(requestTo(BASE + "/api/authorizations?email=outra@example.com"))
				.andRespond(withSuccess("{\"email\": \"outra@example.com\", \"admin\": true}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE + "/api/admins/7?requestedBy=admin@asobi.com.br"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withNoContent());
		server.expect(requestTo(BASE + "/api/authorizations?email=outra@example.com"))
				.andRespond(withSuccess("{\"email\": \"outra@example.com\", \"admin\": false}", MediaType.APPLICATION_JSON));

		assertThat(client.isAdmin("outra@example.com")).isTrue();
		client.removeAdmin(7L, ADMIN);
		assertThat(client.isAdmin("outra@example.com")).isFalse();
		server.verify();
	}

	@Test
	void sendsGoogleTokenAndChecksReturnedEmail() {
		server.expect(requestTo(BASE + "/api/logins"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.idToken").value("token-abc"))
				.andRespond(withSuccess("{\"email\": \"admin@asobi.com.br\", \"admin\": true}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE + "/api/logins"))
				.andRespond(withSuccess("{\"email\": \"outra@example.com\", \"admin\": true}",
						MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE + "/api/logins"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThat(client.authorizeGoogleLogin("token-abc", ADMIN)).isTrue();
		assertThat(client.authorizeGoogleLogin("token-abc", ADMIN)).as("e-mail diferente do token").isFalse();
		assertThat(client.authorizeGoogleLogin("token-abc", ADMIN)).isFalse();
	}

	@Test
	void listsAndAddsAdmins() {
		server.expect(requestTo(BASE + "/api/admins")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("""
						[{"id": 1, "email": "admin@asobi.com.br", "createdByEmail": null, "createdAt": "2026-10-01T12:00:00Z"}]
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE + "/api/admins")).andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.email").value("nova@example.com"))
				.andExpect(jsonPath("$.createdByEmail").value(ADMIN))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"id\": 2, \"email\": \"nova@example.com\", \"createdByEmail\": \"admin@asobi.com.br\"}"));

		assertThat(client.listAdmins()).singleElement()
				.isEqualTo(new AdminAccount(1L, ADMIN, null));
		assertThat(client.addAdmin("nova@example.com", ADMIN).id()).isEqualTo(2L);
		server.verify();
	}

	@Test
	void translatesServiceErrors() {
		server.expect(requestTo(BASE + "/api/admins"))
				.andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
						.body(problem("Esse e-mail já é admin.")));
		server.expect(requestTo(BASE + "/api/admins/9?requestedBy=admin@asobi.com.br"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
						.body("{\"status\": 404, \"detail\": \"Admin não encontrado.\"}"));
		server.expect(requestTo(BASE + "/api/admins")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> client.addAdmin(ADMIN, ADMIN))
				.isInstanceOf(BusinessException.class).hasMessage("Esse e-mail já é admin.");
		assertThatThrownBy(() -> client.removeAdmin(9L, ADMIN))
				.isInstanceOf(NotFoundException.class).hasMessage("Admin não encontrado.");
		assertThatThrownBy(client::listAdmins)
				.isInstanceOf(BusinessException.class).hasMessageContaining("serviço de admins");
	}
}
