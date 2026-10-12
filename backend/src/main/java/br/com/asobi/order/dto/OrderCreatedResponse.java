package br.com.asobi.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedResponse(UUID orderId, Long number, BigDecimal total, String checkoutUrl) {
}
