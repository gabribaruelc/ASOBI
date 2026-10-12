package br.com.asobi.catalog.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Foto de um produto. O arquivo fica no storage; aqui só a chave dele. */
@Entity
@Table(name = "product_images")
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "storage_key", nullable = false, unique = true, length = 200)
	private String storageKey;

	/** Ordem na galeria; a menor é a capa. */
	@Column(nullable = false)
	private int position;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ProductImage() {
	}

	public ProductImage(Product product, String storageKey, int position) {
		this.product = product;
		this.storageKey = storageKey;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
