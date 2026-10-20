package br.com.asobi.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;

/**
 * Público (qualquer um com o link), por isso sem e-mail, telefone ou endereço.
 *
 * @param payable a loja pode mostrar "Pagar agora" (aguardando pagamento, sem pagamento iniciado)
 */
public record OrderStatusResponse(
		UUID orderId,
		Long number,
		OrderStatus status,
		String statusLabel,
		List<Item> items,
		BigDecimal itemsTotal,
		BigDecimal shippingCost,
		String shippingService,
		BigDecimal total,
		String trackingCode,
		Instant createdAt,
		boolean payable) {

	public record Item(String slug, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	public static OrderStatusResponse from(Order order, boolean payable) {
		return new OrderStatusResponse(
				order.getPublicId(),
				order.getId(),
				order.getStatus(),
				order.getStatus().getLabel(),
				order.getItems().stream()
						.map(item -> new Item(item.getProductSlug(), item.getProductName(), item.getQuantity(),
								item.getUnitPrice(), item.getLineTotal()))
						.toList(),
				order.getItemsTotal(),
				order.getShippingCost(),
				order.getShippingService(),
				order.getTotal(),
				order.getTrackingCode(),
				order.getCreatedAt(),
				payable);
	}
}
