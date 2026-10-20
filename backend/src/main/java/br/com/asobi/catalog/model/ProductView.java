package br.com.asobi.catalog.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visualizações da ficha de um produto em um dia (fuso da loja). */
@Entity
@Table(name = "product_views")
public class ProductView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "view_date", nullable = false)
	private LocalDate viewDate;

	@Column(nullable = false)
	private long views;

	protected ProductView() {
	}

	public ProductView(Long productId, LocalDate viewDate) {
		this.productId = productId;
		this.viewDate = viewDate;
		this.views = 1;
	}

	public Long getProductId() {
		return productId;
	}

	public LocalDate getViewDate() {
		return viewDate;
	}

	public long getViews() {
		return views;
	}
}
