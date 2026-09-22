package br.com.asobi.review.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import br.com.asobi.catalog.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Avaliação de cliente. Só aparece na loja depois de aprovada no painel admin. */
@Entity
@Table(name = "reviews")
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "author_name", nullable = false, length = 80)
	private String authorName;

	@Column(nullable = false)
	private int rating;

	@Column(nullable = false, length = 1000)
	private String comment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReviewStatus status = ReviewStatus.PENDING;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Review() {
	}

	public Review(Product product, String authorName, int rating, String comment) {
		this.product = product;
		this.authorName = authorName;
		this.rating = rating;
		this.comment = comment;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getAuthorName() {
		return authorName;
	}

	public int getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public ReviewStatus getStatus() {
		return status;
	}

	public void setStatus(ReviewStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
