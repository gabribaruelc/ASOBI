package br.com.asobi.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

	private final ProductRepository productRepository;
	private final ReviewRepository reviewRepository;

	public AdminDashboardService(ProductRepository productRepository, ReviewRepository reviewRepository) {
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
	}

	public DashboardSummary summary() {
		return new DashboardSummary(
				productRepository.count(),
				productRepository.countByStock(0),
				productRepository.countByOriginalPriceIsNotNull(),
				reviewRepository.countByStatus(ReviewStatus.PENDING));
	}

	public record DashboardSummary(long products, long outOfStock, long onSale, long pendingReviews) {
	}
}
