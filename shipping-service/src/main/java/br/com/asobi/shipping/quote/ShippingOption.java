package br.com.asobi.shipping.quote;

import java.math.BigDecimal;

/** Preço cheio da transportadora; frete grátis é aplicado no backend. */
public record ShippingOption(
		String id,
		String name,
		String company,
		BigDecimal price,
		Integer deliveryDays) {
}
