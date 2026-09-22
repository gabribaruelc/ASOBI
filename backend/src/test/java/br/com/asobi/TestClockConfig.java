package br.com.asobi;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Congela o "agora" dos testes em 22/09/2026 12:00 UTC. */
@TestConfiguration
public class TestClockConfig {

	public static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");

	@Bean
	@Primary
	public Clock fixedClock() {
		return Clock.fixed(NOW, ZoneOffset.UTC);
	}
}
