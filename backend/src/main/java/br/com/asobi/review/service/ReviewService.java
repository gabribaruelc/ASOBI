package br.com.asobi.review.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.review.dto.ReviewRequest;
import br.com.asobi.review.model.Review;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

/** Avaliações: a loja envia, o painel modera. Nada aparece na loja sem aprovação. */
@Service
@Transactional
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final ProductRepository productRepository;

	public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
		this.reviewRepository = reviewRepository;
		this.productRepository = productRepository;
	}

	public Review submit(String productSlug, ReviewRequest request) {
		Product product = productRepository.findBySlug(productSlug)
				.orElseThrow(() -> new NotFoundException("Produto não encontrado: " + productSlug));
		return reviewRepository.save(
				new Review(product, request.name().trim(), request.rating(), request.comment().trim()));
	}

	@Transactional(readOnly = true)
	public List<Review> listByStatus(ReviewStatus status) {
		return reviewRepository.findByStatusOrderByCreatedAtDesc(status);
	}

	@Transactional(readOnly = true)
	public long countByStatus(ReviewStatus status) {
		return reviewRepository.countByStatus(status);
	}

	public void changeStatus(Long id, ReviewStatus status) {
		get(id).setStatus(status);
	}

	public void delete(Long id) {
		reviewRepository.delete(get(id));
	}

	private Review get(Long id) {
		return reviewRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Avaliação não encontrada."));
	}
}
