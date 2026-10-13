package br.com.asobi.catalog.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.dto.ProductViewCount;
import br.com.asobi.catalog.model.ProductView;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

	/** @return 0 se ainda não há linha para esse produto nesse dia */
	@Modifying
	@Transactional
	@Query("update ProductView v set v.views = v.views + 1 where v.productId = :productId and v.viewDate = :date")
	int increment(@Param("productId") Long productId, @Param("date") LocalDate date);

	@Query("select coalesce(sum(v.views), 0) from ProductView v where v.viewDate >= :from")
	long sumViewsSince(@Param("from") LocalDate from);

	@Query("""
			select new br.com.asobi.catalog.dto.ProductViewCount(p.name, p.slug, sum(v.views))
			from ProductView v join Product p on p.id = v.productId
			where v.viewDate >= :from
			group by p.id, p.name, p.slug
			order by sum(v.views) desc, p.name asc
			""")
	List<ProductViewCount> findMostViewedSince(@Param("from") LocalDate from, Pageable pageable);
}
