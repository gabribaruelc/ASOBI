package br.com.asobi.payment;

import java.util.UUID;

/**
 * @param orderPublicId vem do external_reference
 * @param status        status bruto do provedor ("approved", "rejected"...)
 */
public record PaymentUpdate(
		String paymentId,
		UUID orderPublicId,
		String status,
		String method,
		Outcome outcome) {

	public enum Outcome {
		APPROVED,
		PENDING,
		FAILED
	}
}
