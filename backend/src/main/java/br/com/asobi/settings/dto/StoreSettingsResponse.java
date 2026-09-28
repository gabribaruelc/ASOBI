package br.com.asobi.settings.dto;

import java.math.BigDecimal;

public record StoreSettingsResponse(boolean shippingEnabled, BigDecimal freeShippingThreshold) {
}
