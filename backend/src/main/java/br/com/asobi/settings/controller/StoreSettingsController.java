package br.com.asobi.settings.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.alert.ProductAlertGateway;
import br.com.asobi.settings.dto.StoreSettingsResponse;
import br.com.asobi.settings.model.StoreSettings;
import br.com.asobi.settings.service.StoreSettingsService;

@RestController
public class StoreSettingsController {

	private final StoreSettingsService settingsService;
	private final ProductAlertGateway productAlerts;

	public StoreSettingsController(StoreSettingsService settingsService, ProductAlertGateway productAlerts) {
		this.settingsService = settingsService;
		this.productAlerts = productAlerts;
	}

	@GetMapping("/api/settings")
	public StoreSettingsResponse getSettings() {
		StoreSettings settings = settingsService.get();
		return new StoreSettingsResponse(settings.isShippingEnabled(), settings.getFreeShippingThreshold(),
				productAlerts.isEnabled());
	}
}
