package br.com.asobi.catalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.asobi.catalog.model.Product;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	@EntityGraph(attributePaths = "category")
	Optional<Product> findBySlug(String slug);

	@EntityGraph(attributePaths = "category")
	List<Product> findAllByOrderByNameAsc();

	boolean existsBySlug(String slug);

	boolean existsByCategoryId(Long categoryId);

	long countByCategoryId(Long categoryId);

	long countByStock(int stock);

	long countByOriginalPriceIsNotNull();

	@Override
	@EntityGraph(attributePaths = "category")
	List<Product> findAll(Specification<Product> spec, Sort sort);

	List<Product> findBySlugIn(Collection<String> slugs);

	/** Trava a linha do produto até o fim da transação (baixa de estoque sem corrida entre pagamentos). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
