package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.order.dto.OrderRequest.ItemRequest;
import br.com.asobi.settings.model.StoreSettings;
import br.com.asobi.settings.service.StoreSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
public class ShippingController {

	private final ShippingService shippingService;
	private final StoreSettingsService settingsService;

	public ShippingController(ShippingService shippingService, StoreSettingsService settingsService) {
		this.shippingService = shippingService;
		this.settingsService = settingsService;
	}

	/** O que a loja precisa saber para montar o checkout. */
	@GetMapping("/api/settings")
	public StoreSettingsResponse getSettings() {
		StoreSettings settings = settingsService.get();
		return new StoreSettingsResponse(settings.isShippingEnabled(), settings.getFreeShippingThreshold());
	}

	@PostMapping("/api/shipping/quote")
	public List<ShippingOption> quote(@Validated @RequestBody QuoteRequest request) {
		Map<String, Integer> quantities = new LinkedHashMap<>();
		request.items().forEach(item -> quantities.merge(item.slug(), item.quantity(), Integer::sum));
		return shippingService.quote(request.postalCode(), quantities);
	}

	public record StoreSettingsResponse(boolean shippingEnabled, BigDecimal freeShippingThreshold) {
	}

	public record QuoteRequest(
			@NotBlank(message = "Informe o CEP.")
			@Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido.")
			String postalCode,

			@NotEmpty(message = "O carrinho está vazio.")
			@Size(max = 50, message = "Itens demais no carrinho.")
			List<@Valid ItemRequest> items) {
	}
}
