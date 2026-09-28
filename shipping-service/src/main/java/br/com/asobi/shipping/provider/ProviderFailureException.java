package br.com.asobi.shipping.provider;

/** A transportadora respondeu com erro ou não respondeu. Vira HTTP 502. */
public class ProviderFailureException extends RuntimeException {

	public ProviderFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
