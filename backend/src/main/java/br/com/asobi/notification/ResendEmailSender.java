package br.com.asobi.notification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Envio pelo Resend (https://resend.com/docs/api-reference/emails/send-email). */
public class ResendEmailSender implements EmailSender {

	private static final String API_URL = "https://api.resend.com";

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final String from;
	private final String replyTo;

	public ResendEmailSender(EmailProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	/** Usado só nos testes, que trocam o servidor de verdade por um falso. */
	ResendEmailSender(EmailProperties properties, RestClient.Builder builder) {
		this.from = properties.from();
		this.replyTo = properties.replyTo();
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
		post("/emails", toBody(message));
	}

	/** https://resend.com/docs/api-reference/emails/send-batch-emails */
	@Override
	public void sendAll(List<EmailMessage> messages) {
		if (!messages.isEmpty()) {
			post("/emails/batch", messages.stream().map(this::toBody).toList());
		}
	}

	private void post(String uri, Object body) {
		restClient.post()
				.uri(uri)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}

	private Map<String, Object> toBody(EmailMessage message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("from", from);
		body.put("to", message.to());
		body.put("subject", message.subject());
		body.put("html", message.html());
		if (StringUtils.hasText(replyTo)) {
			body.put("reply_to", replyTo);
		}
		if (message.unsubscribeUrl() != null) {
			// com estes cabeçalhos, o Gmail e outros mostram o botão "cancelar inscrição" (padrão RFC 8058)
			body.put("headers", Map.of(
					"List-Unsubscribe", "<" + message.unsubscribeUrl() + ">",
					"List-Unsubscribe-Post", "List-Unsubscribe=One-Click"));
		}
		return body;
	}
}
