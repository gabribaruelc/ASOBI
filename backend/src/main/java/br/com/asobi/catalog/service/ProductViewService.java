package br.com.asobi.catalog.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.model.ProductView;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.catalog.repository.ProductViewRepository;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.common.time.StoreTime;

/** Conta as visualizações da ficha de cada produto, por dia. Não guarda quem viu. */
@Service
public class ProductViewService {

	private final ProductRepository productRepository;
	private final ProductViewRepository productViewRepository;
	private final Clock clock;

	public ProductViewService(ProductRepository productRepository, ProductViewRepository productViewRepository,
			Clock clock) {
		this.productRepository = productRepository;
		this.productViewRepository = productViewRepository;
		this.clock = clock;
	}

	// Sem transação em volta: cada passo confirma sozinho, para a corrida pela primeira
	// visualização do dia poder cair no catch e somar na linha que a outra requisição criou.
	public void recordView(String slug) {
		Product product = productRepository.findBySlug(slug)
				.orElseThrow(() -> new NotFoundException("Jogo não encontrado."));
		LocalDate today = LocalDate.now(clock.withZone(StoreTime.ZONE));
		if (productViewRepository.increment(product.getId(), today) > 0) {
			return;
		}
		try {
			productViewRepository.saveAndFlush(new ProductView(product.getId(), today));
		} catch (DataIntegrityViolationException ex) {
			productViewRepository.increment(product.getId(), today);
		}
	}
}
