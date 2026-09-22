package br.com.asobi.catalog.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.asobi.catalog.model.Product;
import jakarta.persistence.criteria.Predicate;

/** Filtros do catálogo (faixa etária, cooperativos, novidades, promoções). */
public final class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<Product> matching(ProductFilter filter, Instant now) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.category() != null) {
				predicates.add(cb.equal(root.get("category").get("slug"), filter.category()));
			}
			if (Boolean.TRUE.equals(filter.cooperative())) {
				predicates.add(cb.isTrue(root.get("cooperative")));
			}
			if (Boolean.TRUE.equals(filter.isNew())) {
				predicates.add(cb.greaterThan(root.get("newUntil"), now));
			}
			if (Boolean.TRUE.equals(filter.onSale())) {
				predicates.add(cb.isNotNull(root.get("originalPrice")));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	public record ProductFilter(String category, Boolean cooperative, Boolean isNew, Boolean onSale) {
	}
}
