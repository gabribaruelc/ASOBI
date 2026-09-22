package br.com.asobi.payment;

/** Falha ao falar com o provedor de pagamento. Vira HTTP 502 na API. */
public class PaymentException extends RuntimeException {

	public PaymentException(String message, Throwable cause) {
		super(message, cause);
	}
}
