package br.com.asobi.shipping.provider;

/** Cotação impossível por falta de configuração. Vira HTTP 503. */
public class ProviderUnavailableException extends RuntimeException {

	public ProviderUnavailableException(String message) {
		super(message);
	}
}
