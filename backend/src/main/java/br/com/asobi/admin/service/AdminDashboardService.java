package br.com.asobi.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.repository.OrderRepository;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

	private final ProductRepository productRepository;
	private final ReviewRepository reviewRepository;
	private final OrderRepository orderRepository;

	public AdminDashboardService(ProductRepository productRepository, ReviewRepository reviewRepository,
			OrderRepository orderRepository) {
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
		this.orderRepository = orderRepository;
	}

	public DashboardSummary summary() {
		return new DashboardSummary(
				productRepository.count(),
				productRepository.countByStock(0),
				productRepository.countByOriginalPriceIsNotNull(),
				reviewRepository.countByStatus(ReviewStatus.PENDING),
				orderRepository.countByStatus(OrderStatus.PAID),
				orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT));
	}

	public record DashboardSummary(long products, long outOfStock, long onSale, long pendingReviews,
			long ordersToShip, long ordersAwaitingPayment) {
	}
}
