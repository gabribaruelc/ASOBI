package br.com.asobi.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

	long countByStatus(OrderStatus status);
}
