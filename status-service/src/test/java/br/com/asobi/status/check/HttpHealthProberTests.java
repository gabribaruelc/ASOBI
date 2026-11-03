package br.com.asobi.status.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import br.com.asobi.status.check.HealthProber.Probe;

class HttpHealthProberTests {

	private final HttpHealthProber prober = new HttpHealthProber();
	private HttpServer server;
	private String base;

	@BeforeEach
	void start() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.createContext("/down", exchange -> {
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
		server.start();
		base = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	@Test
	void success() {
		assertThat(prober.probe(base + "/ok", Duration.ofSeconds(5))).isEqualTo(Probe.ok());
	}

	@Test
	void errorStatus() {
		assertThat(prober.probe(base + "/down", Duration.ofSeconds(5)))
				.isEqualTo(Probe.failed("respondeu com erro (HTTP 503)"));
	}

	@Test
	void timeout() {
		assertThat(prober.probe(base + "/slow", Duration.ofMillis(300)).detail()).startsWith("não respondeu em");
	}

	@Test
	void nothingListening() {
		int port = server.getAddress().getPort();
		server.stop(0);
		assertThat(prober.probe("http://127.0.0.1:" + port + "/ok", Duration.ofSeconds(2)))
				.isEqualTo(Probe.failed("não foi possível conectar"));
	}
}
