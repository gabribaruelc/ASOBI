package br.com.asobi.twofactor.challenge;

public class EmailDeliveryException extends RuntimeException {

	public EmailDeliveryException(String message) {
		super(message);
	}
}
