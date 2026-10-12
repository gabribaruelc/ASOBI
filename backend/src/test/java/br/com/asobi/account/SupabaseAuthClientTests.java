package br.com.asobi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseAuthClientTests {

	private static final String USER_URL = "https://abc.supabase.co/auth/v1/user";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final SupabaseAuthClient client = new SupabaseAuthClient(
			new CustomerAuthProperties("https://abc.supabase.co/", "secret-key"), builder);

	@Test
	void returnsTheOwnerOfAValidToken() {
		server.expect(requestTo(USER_URL))
				.andExpect(header("apikey", "secret-key"))
				.andExpect(header("Authorization", "Bearer user-token"))
				.andRespond(withSuccess("""
						{"id": "11111111-1111-1111-1111-111111111111", "email": "Maria@Example.com",
						 "email_confirmed_at": "2026-10-01T12:00:00Z", "role": "authenticated"}
						""", MediaType.APPLICATION_JSON));

		assertThat(client.verify("user-token")).contains(new CustomerIdentity(
				UUID.fromString("11111111-1111-1111-1111-111111111111"), "maria@example.com", true));
		server.verify();
	}

	@Test
	void emailWithoutConfirmationIsNotVerified() {
		server.expect(requestTo(USER_URL)).andRespond(withSuccess("""
				{"id": "11111111-1111-1111-1111-111111111111", "email": "maria@example.com"}
				""", MediaType.APPLICATION_JSON));

		assertThat(client.verify("user-token")).get().extracting(CustomerIdentity::emailVerified).isEqualTo(false);
	}

	@Test
	void invalidTokenIsEmpty() {
		server.expect(requestTo(USER_URL)).andRespond(withUnauthorizedRequest());
		assertThat(client.verify("expired")).isEmpty();
	}

	@Test
	void supabaseOutageIsEmpty() {
		server.expect(requestTo(USER_URL)).andRespond(withServerError());
		assertThat(client.verify("user-token")).isEmpty();
	}

	@Test
	void disabledWithoutConfiguration() {
		SupabaseAuthClient disabled = new SupabaseAuthClient(new CustomerAuthProperties("", ""));
		assertThat(disabled.verify("user-token")).isEmpty();
	}
}
