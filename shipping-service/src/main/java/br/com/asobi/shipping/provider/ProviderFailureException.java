package br.com.asobi.shipping.provider;

/** HTTP 502. */
public class ProviderFailureException extends RuntimeException {

	public ProviderFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
