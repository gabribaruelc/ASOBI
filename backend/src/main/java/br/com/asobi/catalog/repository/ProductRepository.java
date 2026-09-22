package br.com.asobi.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.asobi.catalog.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@EntityGraph(attributePaths = "category")
	Optional<Product> findBySlug(String slug);

	boolean existsBySlug(String slug);

	boolean existsByCategoryId(Long categoryId);

	long countByStock(int stock);

	long countByOriginalPriceIsNotNull();

	@Override
	@EntityGraph(attributePaths = "category")
	List<Product> findAll(Specification<Product> spec, Sort sort);
}
