package br.com.asobi.catalog.event;

import java.math.BigDecimal;

import br.com.asobi.catalog.model.Product;

/**
 * Algo mudou num produto pelo painel. Publicado pelo AdminProductService dentro da transação;
 * quem escuta (ex.: avisos por e-mail) decide o que fazer, sem o catálogo depender disso.
 * Leva uma cópia dos dados, porque quem escuta roda depois do commit.
 *
 * @param price         valor cobrado agora (em promoção, o promocional)
 * @param originalPrice preço cheio; só preenchido em promoção
 */
public record ProductEvent(Type type, Long productId, String slug, String name, BigDecimal price,
		BigDecimal originalPrice) {

	public enum Type {
		/** Não estava em promoção e passou a estar. */
		SALE_STARTED,
		/** Estava esgotado e voltou a ter estoque. */
		BACK_IN_STOCK,
		DELETED
	}

	public static ProductEvent of(Type type, Product product) {
		return new ProductEvent(type, product.getId(), product.getSlug(), product.getName(), product.getPrice(),
				product.getOriginalPrice());
	}
}
