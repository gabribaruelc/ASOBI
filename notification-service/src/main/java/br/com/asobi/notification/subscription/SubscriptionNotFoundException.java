package br.com.asobi.notification.subscription;

public class SubscriptionNotFoundException extends RuntimeException {

	public SubscriptionNotFoundException(String message) {
		super(message);
	}
}
