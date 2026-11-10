package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendHealthIndicatorTests {

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final ResendHealthIndicator indicator = new ResendHealthIndicator(
			new EmailProperties("re_123", "ASOBI <pedidos@asobi.com.br>", null), builder);

	private Status statusWhenDomainsAre(String json) {
		server.expect(requestTo("https://api.resend.com/domains"))
				.andExpect(header("Authorization", "Bearer re_123"))
				.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
		return indicator.health().getStatus();
	}

	@Test
	void upWhenSenderDomainIsVerified() {
		assertThat(statusWhenDomainsAre("{\"data\":[{\"name\":\"asobi.com.br\",\"status\":\"verified\"}]}"))
				.isEqualTo(Status.UP);
	}

	@Test
	void downWhenSenderDomainLostVerification() {
		assertThat(statusWhenDomainsAre("{\"data\":[{\"name\":\"asobi.com.br\",\"status\":\"failed\"}]}"))
				.isEqualTo(Status.DOWN);
	}

	@Test
	void sendOnlyKeyCountsAsUp() {
		server.expect(requestTo("https://api.resend.com/domains"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"name\":\"restricted_api_key\",\"message\":\"This API key is restricted to only send emails\"}"));
		assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void downWhenKeyIsRejected() {
		server.expect(requestTo("https://api.resend.com/domains"))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"name\":\"validation_error\",\"message\":\"API key is invalid\"}"));
		assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	void downWhenResendIsUnavailable() {
		server.expect(requestTo("https://api.resend.com/domains")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
		assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}
}
