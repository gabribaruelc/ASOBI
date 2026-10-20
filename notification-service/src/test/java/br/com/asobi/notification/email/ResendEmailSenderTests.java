package br.com.asobi.notification.email;

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

	private static final List<EmailMessage> MESSAGES = List.of(
			new EmailMessage("maria@example.com", "Assunto", "<p>Oi, Maria</p>", "https://avisos.asobi.test/descadastro/aaa"),
			new EmailMessage("joao@example.com", "Assunto", "<p>Oi, João</p>", "https://avisos.asobi.test/descadastro/bbb"));

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	private ResendEmailSender sender(String replyTo) {
		return new ResendEmailSender(new EmailProperties("re_123", "ASOBI <avisos@asobi.com.br>", replyTo), builder);
	}

	@Test
	void postsOneBatchWithAnUnsubscribeHeaderPerEmail() {
		server.expect(requestTo("https://api.resend.com/emails/batch"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer re_123"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].from").value("ASOBI <avisos@asobi.com.br>"))
				.andExpect(jsonPath("$[0].to[0]").value("maria@example.com"))
				.andExpect(jsonPath("$[0].subject").value("Assunto"))
				.andExpect(jsonPath("$[0].html").value("<p>Oi, Maria</p>"))
				.andExpect(jsonPath("$[0].reply_to").value("contato@asobi.com.br"))
				.andExpect(jsonPath("$[0].headers['List-Unsubscribe']")
						.value("<https://avisos.asobi.test/descadastro/aaa>"))
				.andExpect(jsonPath("$[0].headers['List-Unsubscribe-Post']").value("List-Unsubscribe=One-Click"))
				.andExpect(jsonPath("$[1].to[0]").value("joao@example.com"))
				.andExpect(jsonPath("$[1].headers['List-Unsubscribe']")
						.value("<https://avisos.asobi.test/descadastro/bbb>"))
				.andRespond(withSuccess("{\"data\": [{\"id\": \"a\"}, {\"id\": \"b\"}]}", MediaType.APPLICATION_JSON));

		sender("contato@asobi.com.br").sendAll(MESSAGES);

		server.verify();
	}

	@Test
	void omitsReplyToWhenNotConfigured() {
		server.expect(requestTo("https://api.resend.com/emails/batch"))
				.andExpect(jsonPath("$[0].reply_to").doesNotExist())
				.andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

		sender("").sendAll(MESSAGES);

		server.verify();
	}

	@Test
	void emptyBatchDoesNotCallResend() {
		sender("").sendAll(List.of());

		server.verify();
	}

	@Test
	void providerErrorIsThrown() {
		server.expect(requestTo("https://api.resend.com/emails/batch")).andRespond(withStatus(HttpStatus.FORBIDDEN)
				.contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"The domain is not verified\"}"));

		assertThatThrownBy(() -> sender("").sendAll(MESSAGES)).isInstanceOf(RestClientException.class);
	}
}
