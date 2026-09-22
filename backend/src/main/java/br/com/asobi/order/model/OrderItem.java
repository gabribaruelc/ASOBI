package br.com.asobi.order.model;

import java.math.BigDecimal;

import br.com.asobi.catalog.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Item do pedido, com nome e preço congelados no momento da compra. */
@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	/** Pode ficar nulo se o produto for excluído depois. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "product_slug", nullable = false, length = 120)
	private String productSlug;

	@Column(name = "product_name", nullable = false, length = 150)
	private String productName;

	@Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "line_total", nullable = false, precision = 10, scale = 2)
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	OrderItem(Order order, Product product, int quantity) {
		this.order = order;
		this.product = product;
		this.productSlug = product.getSlug();
		this.productName = product.getName();
		this.unitPrice = product.getPrice();
		this.quantity = quantity;
		this.lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getProductSlug() {
		return productSlug;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
