package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class ResendEmailSenderTests {

	private static final EmailMessage MESSAGE = new EmailMessage(List.of("maria@example.com"), "Assunto", "<p>Oi</p>");

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	private ResendEmailSender sender(String replyTo) {
		return new ResendEmailSender(new EmailProperties("re_123", "ASOBI <pedidos@asobi.com.br>", replyTo), builder);
	}

	@Test
	void postsEmailToResend() {
		server.expect(requestTo("https://api.resend.com/emails"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer re_123"))
				.andExpect(jsonPath("$.from").value("ASOBI <pedidos@asobi.com.br>"))
				.andExpect(jsonPath("$.to[0]").value("maria@example.com"))
				.andExpect(jsonPath("$.subject").value("Assunto"))
				.andExpect(jsonPath("$.html").value("<p>Oi</p>"))
				.andExpect(jsonPath("$.reply_to").value("contato@asobi.com.br"))
				.andRespond(withSuccess("{\"id\": \"abc\"}", MediaType.APPLICATION_JSON));

		sender("contato@asobi.com.br").send(MESSAGE);

		server.verify();
	}

	@Test
	void omitsReplyToWhenNotConfigured() {
		server.expect(requestTo("https://api.resend.com/emails"))
				.andExpect(jsonPath("$.reply_to").doesNotExist())
				.andRespond(withSuccess("{\"id\": \"abc\"}", MediaType.APPLICATION_JSON));

		sender("").send(MESSAGE);

		server.verify();
	}

	@Test
	void providerErrorIsThrown() {
		server.expect(requestTo("https://api.resend.com/emails")).andRespond(withStatus(HttpStatus.FORBIDDEN)
				.contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"The domain is not verified\"}"));

		assertThatThrownBy(() -> sender("").send(MESSAGE)).isInstanceOf(RestClientException.class);
	}
}
