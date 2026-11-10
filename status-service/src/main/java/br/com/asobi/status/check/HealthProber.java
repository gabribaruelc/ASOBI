package br.com.asobi.status.check;

import java.time.Duration;
import java.util.Map;

/** Faz uma conferência: o endereço respondeu com sucesso dentro do tempo? */
public interface HealthProber {

	/**
	 * @param detail     o que deu errado, em português (null quando está no ar)
	 * @param components partes informadas pelo /actuator/health (ex.: db → DOWN, resend → UP)
	 */
	record Probe(boolean up, String detail, Map<String, String> components) {

		public Probe {
			components = components == null ? Map.of() : Map.copyOf(components);
		}

		public static Probe ok() {
			return new Probe(true, null, Map.of());
		}

		public static Probe ok(Map<String, String> components) {
			return new Probe(true, null, components);
		}

		public static Probe failed(String detail) {
			return new Probe(false, detail, Map.of());
		}
	}

	Probe probe(String url, Duration timeout);
}
