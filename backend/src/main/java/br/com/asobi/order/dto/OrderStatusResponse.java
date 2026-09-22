package br.com.asobi.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;

/**
 * Acompanhamento do pedido pelo cliente (página "pedido recebido").
 * Sem e-mail, telefone ou endereço: só o necessário para acompanhar.
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
		Instant createdAt) {

	public record Item(String slug, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}

	public static OrderStatusResponse from(Order order) {
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
				order.getCreatedAt());
	}
}
