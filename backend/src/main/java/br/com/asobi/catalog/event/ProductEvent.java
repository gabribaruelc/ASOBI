package br.com.asobi.catalog.event;

import java.math.BigDecimal;

import br.com.asobi.catalog.model.Product;

/**
 * Avisa que algo mudou num jogo: pelo painel (AdminProductService) ou por uma venda (OrderService,
 * que manda o {@link Type#LOW_STOCK}). É anunciado no meio da gravação; quem está "escutando"
 * (ex.: os avisos por e-mail) decide o que fazer, e o catálogo não precisa saber quem é.
 * Leva uma cópia dos dados, porque quem escuta só roda depois que tudo foi salvo no banco.
 *
 * @param price         valor cobrado agora (em promoção, o promocional)
 * @param originalPrice preço cheio; só preenchido em promoção
 * @param stock         unidades depois da mudança
 */
public record ProductEvent(Type type, Long productId, String slug, String name, BigDecimal price,
		BigDecimal originalPrice, int stock) {

	public enum Type {
		/** Não estava em promoção e passou a estar. */
		SALE_STARTED,
		/** Estava esgotado e voltou a ter estoque. */
		BACK_IN_STOCK,
		/** Uma venda deixou o jogo no limite de estoque baixo ou abaixo dele (pode ter esgotado). */
		LOW_STOCK,
		DELETED
	}

	public static ProductEvent of(Type type, Product product) {
		return new ProductEvent(type, product.getId(), product.getSlug(), product.getName(), product.getPrice(),
				product.getOriginalPrice(), product.getStock());
	}
}
