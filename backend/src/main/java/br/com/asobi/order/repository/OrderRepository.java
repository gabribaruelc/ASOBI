package br.com.asobi.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.asobi.order.dto.ProductSales;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = "items")
	Optional<Order> findByPublicId(UUID publicId);

	@EntityGraph(attributePaths = "items")
	Optional<Order> findWithItemsById(Long id);

	@EntityGraph(attributePaths = "items")
	List<Order> findAllByOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = "items")
	List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

	/** Pedidos feitos logado nessa conta, mais os feitos sem cadastro com o mesmo e-mail. */
	@EntityGraph(attributePaths = "items")
	@Query("select o from Order o where o.customerUserId = :userId or o.customer.email = :email order by o.createdAt desc")
	List<Order> findForCustomer(@Param("userId") UUID userId, @Param("email") String email);

	@EntityGraph(attributePaths = "items")
	List<Order> findByCustomerUserIdOrderByCreatedAtDesc(UUID customerUserId);

	long countByStatus(OrderStatus status);

	/** Pedidos com pagamento aprovado a partir de {@code from}, sem os itens. */
	@Query("select o from Order o where o.paidAt >= :from order by o.paidAt")
	List<Order> findPaidSince(@Param("from") Instant from);

	long countByCreatedAtGreaterThanEqual(Instant from);

	@Query("""
			select new br.com.asobi.order.dto.ProductSales(i.productName, sum(i.quantity), sum(i.lineTotal))
			from OrderItem i
			where i.order.paidAt >= :from
			group by i.productSlug, i.productName
			order by sum(i.quantity) desc, i.productName asc
			""")
	List<ProductSales> findBestSellersSince(@Param("from") Instant from, Pageable pageable);
}
