package br.com.asobi.payment;

/** HTTP 502 na API. */
public class PaymentException extends RuntimeException {

	public PaymentException(String message, Throwable cause) {
		super(message, cause);
	}
}
