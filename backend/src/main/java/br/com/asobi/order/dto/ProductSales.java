package br.com.asobi.order.dto;

import java.math.BigDecimal;

public record ProductSales(String name, Long quantity, BigDecimal revenue) {
}
