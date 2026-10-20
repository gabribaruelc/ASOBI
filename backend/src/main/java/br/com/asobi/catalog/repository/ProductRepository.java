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

	List<Product> findByStockBetweenOrderByStockAscNameAsc(int min, int max);

	List<Product> findByOriginalPriceIsNotNullAndStockGreaterThanOrderByNameAsc(int stock);

	@Override
	@EntityGraph(attributePaths = "category")
	List<Product> findAll(Specification<Product> spec, Sort sort);

	List<Product> findBySlugIn(Collection<String> slugs);

	/** SELECT ... FOR UPDATE, para dois pagamentos não baixarem o mesmo estoque ao mesmo tempo. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
