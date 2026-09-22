package br.com.asobi.payment;

import java.util.UUID;

/**
 * Situação de um pagamento segundo o provedor.
 *
 * @param orderPublicId pedido ao qual o pagamento pertence (external_reference)
 * @param status        status bruto do provedor (ex.: "approved", "rejected")
 * @param outcome       o que isso significa para o pedido
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
