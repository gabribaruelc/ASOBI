package br.com.asobi.status.check;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.springframework.stereotype.Component;

/** GET simples; qualquer resposta 2xx conta como no ar. */
@Component
public class HttpHealthProber implements HealthProber {

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	@Override
	public Probe probe(String url, Duration timeout) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(timeout)
				.header("User-Agent", "asobi-status-service")
				.GET()
				.build();
		try {
			int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
			return status >= 200 && status < 300 ? Probe.ok() : Probe.failed("respondeu com erro (HTTP " + status + ")");
		} catch (HttpTimeoutException ex) {
			return Probe.failed("não respondeu em " + timeout.toSeconds() + " segundos");
		} catch (IOException ex) {
			return Probe.failed("não foi possível conectar");
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return Probe.failed("conferência interrompida");
		}
	}
}
