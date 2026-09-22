package br.com.asobi.settings.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Configurações da loja (linha única, id = 1). */
@Entity
@Table(name = "store_settings")
public class StoreSettings {

	public static final long SINGLETON_ID = 1L;

	@Id
	private Long id;

	@Column(name = "shipping_enabled", nullable = false)
	private boolean shippingEnabled;

	@Column(name = "origin_postal_code", length = 9)
	private String originPostalCode;

	@Column(name = "free_shipping_threshold", precision = 10, scale = 2)
	private BigDecimal freeShippingThreshold;

	@Column(name = "package_weight_kg", nullable = false, precision = 6, scale = 3)
	private BigDecimal packageWeightKg;

	@Column(name = "package_width_cm", nullable = false)
	private int packageWidthCm;

	@Column(name = "package_height_cm", nullable = false)
	private int packageHeightCm;

	@Column(name = "package_length_cm", nullable = false)
	private int packageLengthCm;

	@Column(name = "updated_by_email", length = 254)
	private String updatedByEmail;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StoreSettings() {
	}

	/** Frete grátis vale para este subtotal? */
	public boolean qualifiesForFreeShipping(BigDecimal itemsTotal) {
		return freeShippingThreshold != null && itemsTotal.compareTo(freeShippingThreshold) >= 0;
	}

	public Long getId() {
		return id;
	}

	public boolean isShippingEnabled() {
		return shippingEnabled;
	}

	public void setShippingEnabled(boolean shippingEnabled) {
		this.shippingEnabled = shippingEnabled;
	}

	public String getOriginPostalCode() {
		return originPostalCode;
	}

	public void setOriginPostalCode(String originPostalCode) {
		this.originPostalCode = originPostalCode;
	}

	public BigDecimal getFreeShippingThreshold() {
		return freeShippingThreshold;
	}

	public void setFreeShippingThreshold(BigDecimal freeShippingThreshold) {
		this.freeShippingThreshold = freeShippingThreshold;
	}

	public BigDecimal getPackageWeightKg() {
		return packageWeightKg;
	}

	public void setPackageWeightKg(BigDecimal packageWeightKg) {
		this.packageWeightKg = packageWeightKg;
	}

	public int getPackageWidthCm() {
		return packageWidthCm;
	}

	public void setPackageWidthCm(int packageWidthCm) {
		this.packageWidthCm = packageWidthCm;
	}

	public int getPackageHeightCm() {
		return packageHeightCm;
	}

	public void setPackageHeightCm(int packageHeightCm) {
		this.packageHeightCm = packageHeightCm;
	}

	public int getPackageLengthCm() {
		return packageLengthCm;
	}

	public void setPackageLengthCm(int packageLengthCm) {
		this.packageLengthCm = packageLengthCm;
	}

	public String getUpdatedByEmail() {
		return updatedByEmail;
	}

	public void setUpdatedByEmail(String updatedByEmail) {
		this.updatedByEmail = updatedByEmail;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
