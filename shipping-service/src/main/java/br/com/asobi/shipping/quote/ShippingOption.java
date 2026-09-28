package br.com.asobi.shipping.quote;

import java.math.BigDecimal;

/** Opção de entrega cotada (preço cheio da transportadora, sem desconto da loja). */
public record ShippingOption(
		String id,
		String name,
		String company,
		BigDecimal price,
		Integer deliveryDays) {
}
