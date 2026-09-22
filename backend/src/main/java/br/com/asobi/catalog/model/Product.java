package br.com.asobi.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 16)
	private String icon;

	@Column(nullable = false, length = 4000)
	private String description;

	@Column(nullable = false, length = 100)
	private String skill;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "age_label", nullable = false, length = 40)
	private String ageLabel;

	@Column(nullable = false, length = 60)
	private String players;

	/** Preço de venda. Em promoção, é o preço promocional. */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	/** Preço "de" (riscado). Preenchido apenas quando o produto está em promoção. */
	@Column(name = "original_price", precision = 10, scale = 2)
	private BigDecimal originalPrice;

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false)
	private boolean cooperative;

	/** Até quando o produto aparece em Novidades; null = não é novidade. */
	@Column(name = "new_until")
	private Instant newUntil;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public boolean isOnSale() {
		return originalPrice != null;
	}

	public boolean isInStock() {
		return stock > 0;
	}

	public boolean isNewAt(Instant now) {
		return newUntil != null && newUntil.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSkill() {
		return skill;
	}

	public void setSkill(String skill) {
		this.skill = skill;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getAgeLabel() {
		return ageLabel;
	}

	public void setAgeLabel(String ageLabel) {
		this.ageLabel = ageLabel;
	}

	public String getPlayers() {
		return players;
	}

	public void setPlayers(String players) {
		this.players = players;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(BigDecimal originalPrice) {
		this.originalPrice = originalPrice;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public boolean isCooperative() {
		return cooperative;
	}

	public void setCooperative(boolean cooperative) {
		this.cooperative = cooperative;
	}

	public Instant getNewUntil() {
		return newUntil;
	}

	public void setNewUntil(Instant newUntil) {
		this.newUntil = newUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
