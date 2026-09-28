package br.com.asobi.shipping.quote;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.shipping.provider.ShippingQuoteProvider;

@RestController
@RequestMapping("/api")
public class QuoteController {

	private final ShippingQuoteProvider provider;

	public QuoteController(ShippingQuoteProvider provider) {
		this.provider = provider;
	}

	@GetMapping("/provider")
	public ProviderStatusResponse getProvider() {
		return new ProviderStatusResponse(provider.name(), provider.isConfigured());
	}

	/** Opções da mais barata para a mais cara. */
	@PostMapping("/quotes")
	public List<ShippingOption> quote(@Validated @RequestBody QuoteRequest request) {
		return provider.quote(request);
	}
}
