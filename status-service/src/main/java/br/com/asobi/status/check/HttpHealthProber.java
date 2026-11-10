package br.com.asobi.status.check;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * GET simples; qualquer resposta 2xx conta como no ar. Se a resposta é de um /actuator/health
 * com "components", o serviço respondeu e o que caiu fica nas partes (ex.: só o banco).
 */
@Component
public class HttpHealthProber implements HealthProber {

	// HTTP/1.1: sem isso o Java tenta o upgrade para h2c e o Next em modo dev fecha a conexão
	private final HttpClient client = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Override
	public Probe probe(String url, Duration timeout) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(timeout)
				.header("User-Agent", "asobi-status-service")
				.GET()
				.build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			Map<String, String> components = components(response);
			if (!components.isEmpty()) {
				return Probe.ok(components);
			}
			int status = response.statusCode();
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

	private Map<String, String> components(HttpResponse<String> response) {
		boolean json = response.headers().firstValue("Content-Type").map(type -> type.contains("json")).orElse(false);
		if (!json) {
			return Map.of();
		}
		try {
			JsonNode components = jsonMapper.readTree(response.body()).path("components");
			Map<String, String> result = new LinkedHashMap<>();
			components.properties().forEach(entry -> result.put(entry.getKey(), entry.getValue().path("status").asString("UNKNOWN")));
			return result;
		} catch (JacksonException ex) {
			return Map.of();
		}
	}
}
