package br.com.asobi.review.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.review.model.Review;
import br.com.asobi.review.model.ReviewStatus;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByProductIdInAndStatusOrderByCreatedAtAsc(Collection<Long> productIds, ReviewStatus status);

	long countByStatus(ReviewStatus status);
}
