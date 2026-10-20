package br.com.asobi.payment;

import java.util.Optional;

import br.com.asobi.order.model.Order;

public interface PaymentGateway {

	String name();

	/** @return URL de pagamento para onde o cliente é redirecionado */
	String createCheckout(Order order);

	Optional<PaymentUpdate> fetchPayment(String paymentId);
}
