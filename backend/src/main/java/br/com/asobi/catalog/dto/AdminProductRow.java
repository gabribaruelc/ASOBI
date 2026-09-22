package br.com.asobi.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.common.time.StoreTime;

/** Linha da lista de produtos do painel. */
public record AdminProductRow(
		Long id,
		String slug,
		String icon,
		String name,
		String categoryName,
		BigDecimal price,
		BigDecimal originalPrice,
		int stock,
		boolean cooperative,
		boolean isNew,
		LocalDate newUntil,
		long newDaysRemaining) {

	public static AdminProductRow from(Product product, Instant now) {
		return new AdminProductRow(
				product.getId(),
				product.getSlug(),
				product.getIcon(),
				product.getName(),
				product.getCategory().getName(),
				product.getPrice(),
				product.getOriginalPrice(),
				product.getStock(),
				product.isCooperative(),
				product.isNewAt(now),
				StoreTime.lastDay(product.getNewUntil()),
				StoreTime.daysRemaining(product.getNewUntil(), now));
	}
}
