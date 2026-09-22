package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.settings.model.StoreSettings;
import br.com.asobi.settings.service.StoreSettingsService;

/**
 * Frete automático. Só funciona com o toggle ligado no painel. O preço sempre
 * vem de uma cotação feita pelo servidor — no checkout e de novo ao fechar o pedido.
 */
@Service
@Transactional(readOnly = true)
public class ShippingService {

	private final ShippingQuoteProvider provider;
	private final StoreSettingsService settingsService;
	private final ProductRepository productRepository;

	public ShippingService(ShippingQuoteProvider provider, StoreSettingsService settingsService,
			ProductRepository productRepository) {
		this.provider = provider;
		this.settingsService = settingsService;
		this.productRepository = productRepository;
	}

	public boolean isEnabled() {
		return settingsService.get().isShippingEnabled();
	}

	/**
	 * @param quantities slug → quantidade
	 * @return opções da mais barata para a mais cara; com frete grátis, a mais barata sai por R$ 0
	 */
	public List<ShippingOption> quote(String destinationPostalCode, Map<String, Integer> quantities) {
		StoreSettings settings = settingsService.get();
		if (!settings.isShippingEnabled()) {
			throw new BusinessException("O cálculo automático de frete está desligado.");
		}
		List<Product> products = productRepository.findBySlugIn(quantities.keySet());
		BigDecimal itemsTotal = BigDecimal.ZERO;
		List<ShippingQuoteProvider.Parcel> parcels = new ArrayList<>();
		for (Product product : products) {
			int quantity = quantities.get(product.getSlug());
			itemsTotal = itemsTotal.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
			parcels.add(new ShippingQuoteProvider.Parcel(product.getSlug(), quantity, product.getPrice(),
					settings.getPackageWeightKg(), settings.getPackageWidthCm(), settings.getPackageHeightCm(),
					settings.getPackageLengthCm()));
		}
		if (parcels.isEmpty()) {
			throw new BusinessException("O carrinho está vazio.");
		}

		List<ShippingOption> options = new ArrayList<>(provider.quote(new ShippingQuoteProvider.QuoteRequest(
				settings.getOriginPostalCode(), destinationPostalCode, parcels)));
		if (options.isEmpty()) {
			throw new BusinessException("Nenhuma transportadora entrega nesse CEP. Confira o CEP ou fale com a gente.");
		}
		if (settings.qualifiesForFreeShipping(itemsTotal)) {
			options.set(0, options.get(0).asFree());
		}
		return options;
	}

	/** Recota e devolve a opção escolhida no checkout (o preço nunca vem do navegador). */
	public ShippingOption resolve(String destinationPostalCode, Map<String, Integer> quantities, String optionId) {
		return quote(destinationPostalCode, quantities).stream()
				.filter(option -> option.id().equals(optionId))
				.findFirst()
				.orElseThrow(() -> new BusinessException(
						"A opção de frete escolhida não está mais disponível. Escolha o frete de novo."));
	}
}
