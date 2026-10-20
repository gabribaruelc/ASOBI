package br.com.asobi.alert;

import org.springframework.stereotype.Service;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;

@Service
public class ProductAlertService {

	private final ProductAlertGateway gateway;
	private final ProductRepository productRepository;

	public ProductAlertService(ProductAlertGateway gateway, ProductRepository productRepository) {
		this.gateway = gateway;
		this.productRepository = productRepository;
	}

	/** Só aceita o aviso que ainda faz sentido: o jogo esgotado, ou fora de promoção. */
	public void subscribe(ProductAlertRequest request) {
		Product product = productRepository.findBySlug(request.productSlug())
				.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
		if (request.type() == AlertType.BACK_IN_STOCK && product.isInStock()) {
			throw new BusinessException("Este jogo já está disponível.");
		}
		if (request.type() == AlertType.PROMOTION && product.isOnSale()) {
			throw new BusinessException("Este jogo já está em promoção.");
		}
		gateway.subscribe(request.email().trim(), product.getId(), request.type());
	}
}
