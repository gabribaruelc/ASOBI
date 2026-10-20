package br.com.asobi.shipping.provider;

/** Sem token configurado. HTTP 503. */
public class ProviderUnavailableException extends RuntimeException {

	public ProviderUnavailableException(String message) {
		super(message);
	}
}
