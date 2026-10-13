package br.com.asobi.payment;

import java.util.Optional;

import br.com.asobi.order.model.Order;

/** Sem Mercado Pago: o pagamento é confirmado à mão no painel. */
public class ManualPaymentGateway implements PaymentGateway {

	private final String storeUrl;

	public ManualPaymentGateway(String storeUrl) {
		this.storeUrl = storeUrl.replaceAll("/$", "");
	}

	@Override
	public String name() {
		return "manual";
	}

	@Override
	public String createCheckout(Order order) {
		return storeUrl + "/pedido/" + order.getPublicId();
	}

	@Override
	public Optional<PaymentUpdate> fetchPayment(String paymentId) {
		return Optional.empty();
	}
}
