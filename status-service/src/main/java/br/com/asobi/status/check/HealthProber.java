package br.com.asobi.status.check;

import java.time.Duration;

/** Faz uma conferência: o endereço respondeu com sucesso dentro do tempo? */
public interface HealthProber {

	/** @param detail o que deu errado, em português (null quando está no ar) */
	record Probe(boolean up, String detail) {

		public static Probe ok() {
			return new Probe(true, null);
		}

		public static Probe failed(String detail) {
			return new Probe(false, detail);
		}
	}

	Probe probe(String url, Duration timeout);
}
