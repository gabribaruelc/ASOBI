package br.com.asobi.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.review.model.Review;

/**
 * Produto como a loja enxerga. Não expõe a quantidade exata em estoque, só se
 * há estoque; e traz apenas as avaliações já aprovadas. Sem fotos ("images" vazio),
 * a loja mostra o emoji de "icon".
 */
public record ProductResponse(
		String slug,
		String name,
		String icon,
		List<String> images,
		String description,
		String skill,
		String ageKey,
		String age,
		String players,
		BigDecimal price,
		Promo promo,
		boolean inStock,
		boolean cooperative,
		Instant newUntil,
		boolean isNew,
		List<ReviewResponse> reviews) {

	public record Promo(BigDecimal originalPrice) {
	}

	public record ReviewResponse(Long id, String name, int rating, String comment) {

		public static ReviewResponse from(Review review) {
			return new ReviewResponse(review.getId(), review.getAuthorName(), review.getRating(), review.getComment());
		}
	}

	/** @param imageUrls URLs das fotos na ordem da galeria (a primeira é a capa) */
	public static ProductResponse from(Product product, List<String> imageUrls, List<Review> approvedReviews,
			Instant now) {
		return new ProductResponse(
				product.getSlug(),
				product.getName(),
				product.getIcon(),
				imageUrls,
				product.getDescription(),
				product.getSkill(),
				product.getCategory().getSlug(),
				product.getAgeLabel(),
				product.getPlayers(),
				product.getPrice(),
				product.isOnSale() ? new Promo(product.getOriginalPrice()) : null,
				product.isInStock(),
				product.isCooperative(),
				product.getNewUntil(),
				product.isNewAt(now),
				approvedReviews.stream().map(ReviewResponse::from).toList());
	}
}
