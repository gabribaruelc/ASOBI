package br.com.asobi.shipping;

import java.math.BigDecimal;

/**
 * Opção de entrega mostrada no checkout.
 *
 * @param originalPrice preço sem desconto, quando o frete grátis foi aplicado (senão null)
 */
public record ShippingOption(
		String id,
		String name,
		String company,
		BigDecimal price,
		BigDecimal originalPrice,
		Integer deliveryDays) {

	public ShippingOption asFree() {
		return new ShippingOption(id, name, company, BigDecimal.ZERO, price, deliveryDays);
	}

	/** Ex.: "Correios PAC · 5 dias úteis" (gravado no pedido). */
	public String label() {
		String base = company == null ? name : company + " " + name;
		return deliveryDays == null ? base : base + " · " + deliveryDays + " dias úteis";
	}
}
