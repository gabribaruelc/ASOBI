package br.com.asobi.twofactor.email;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Envio pelo Resend (https://resend.com/docs/api-reference/emails/send-email). */
public class ResendEmailSender implements EmailSender {

	private static final String API_URL = "https://api.resend.com";

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final String from;

	public ResendEmailSender(EmailProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	/** Construtor para testes (MockRestServiceServer). */
	ResendEmailSender(EmailProperties properties, RestClient.Builder builder) {
		this.from = properties.from();
		this.restClient = builder
				.baseUrl(API_URL)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resendApiKey())
				.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	@Override
	public void send(EmailMessage message) {
		Map<String, Object> body = Map.of(
				"from", from,
				"to", List.of(message.to()),
				"subject", message.subject(),
				"html", message.html(),
				"text", message.text());
		restClient.post()
				.uri("/emails")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}
}
