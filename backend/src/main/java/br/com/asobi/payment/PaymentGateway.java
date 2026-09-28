package br.com.asobi.payment;

import java.util.Optional;

import br.com.asobi.order.model.Order;

/**
 * Provedor de pagamento. Em produção, Mercado Pago Checkout Pro; sem
 * credenciais configuradas, o modo manual (a Priscila confirma no painel).
 */
public interface PaymentGateway {

	/** Nome gravado no pedido (ex.: "mercadopago", "manual"). */
	String name();

	/** Cria a cobrança e devolve a URL para onde o cliente vai pagar. */
	String createCheckout(Order order);

	/** Consulta um pagamento no provedor (usado pelo webhook). */
	Optional<PaymentUpdate> fetchPayment(String paymentId);
}
