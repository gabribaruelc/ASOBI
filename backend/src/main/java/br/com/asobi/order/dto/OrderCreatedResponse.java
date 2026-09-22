package br.com.asobi.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** @param checkoutUrl para onde a loja deve mandar o cliente pagar */
public record OrderCreatedResponse(UUID orderId, Long number, BigDecimal total, String checkoutUrl) {
}
