package br.com.asobi.alert;

import org.springframework.stereotype.Service;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.settings.model.StoreSettings;
import br.com.asobi.settings.service.StoreSettingsService;

@Service
public class ProductAlertService {

	private final ProductAlertGateway gateway;
	private final ProductRepository productRepository;
	private final StoreSettingsService settingsService;

	public ProductAlertService(ProductAlertGateway gateway, ProductRepository productRepository,
			StoreSettingsService settingsService) {
		this.gateway = gateway;
		this.productRepository = productRepository;
		this.settingsService = settingsService;
	}

	/**
	 * Só aceita o aviso que ainda faz sentido: o jogo esgotado, fora de promoção, ou com estoque
	 * acima do limite de "últimas unidades".
	 */
	public void subscribe(ProductAlertRequest request) {
		Product product = productRepository.findBySlug(request.productSlug())
				.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		if (request.type() == AlertType.BACK_IN_STOCK && product.isInStock()) {
			throw new BusinessException("Este jogo já está disponível.");
		}
		if (request.type() == AlertType.PROMOTION && product.isOnSale()) {
			throw new BusinessException("Este jogo já está em promoção.");
		}
		if (request.type() == AlertType.LOW_STOCK) {
			StoreSettings settings = settingsService.get();
			if (settings.getLowStockThreshold() == 0) {
				throw new BusinessException("Este aviso não está disponível.");
			}
			if (!product.isInStock()) {
				throw new BusinessException("Este jogo esgotou. Peça o aviso de quando voltar.");
			}
			if (settings.isLowStock(product.getStock())) {
				throw new BusinessException("Este jogo já está nas últimas unidades: garanta o seu!");
			}
		}
		gateway.subscribe(request.email().trim(), product.getId(), request.type());
	}
}
