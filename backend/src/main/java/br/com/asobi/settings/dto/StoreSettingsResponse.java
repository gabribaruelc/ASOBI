package br.com.asobi.settings.dto;

import java.math.BigDecimal;

/** @param productAlertsEnabled a loja pode mostrar o "avise-me" na ficha do jogo */
public record StoreSettingsResponse(boolean shippingEnabled, BigDecimal freeShippingThreshold,
		boolean productAlertsEnabled) {
}
