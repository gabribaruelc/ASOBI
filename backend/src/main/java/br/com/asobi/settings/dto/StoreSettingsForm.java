package br.com.asobi.settings.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class StoreSettingsForm {

	private boolean shippingEnabled;

	@Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP inválido.")
	private String originPostalCode;

	@DecimalMin(value = "0.00", message = "Valor inválido.")
	private BigDecimal freeShippingThreshold;

	@NotNull(message = "Informe o peso.")
	@DecimalMin(value = "0.010", message = "Peso mínimo de 10 g.")
	@DecimalMax(value = "30.000", message = "Peso máximo de 30 kg.")
	private BigDecimal packageWeightKg;

	@NotNull(message = "Informe a largura.")
	@Min(value = 1, message = "Mínimo 1 cm.")
	@Max(value = 100, message = "Máximo 100 cm.")
	private Integer packageWidthCm;

	@NotNull(message = "Informe a altura.")
	@Min(value = 1, message = "Mínimo 1 cm.")
	@Max(value = 100, message = "Máximo 100 cm.")
	private Integer packageHeightCm;

	@NotNull(message = "Informe o comprimento.")
	@Min(value = 1, message = "Mínimo 1 cm.")
	@Max(value = 100, message = "Máximo 100 cm.")
	private Integer packageLengthCm;

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

	public Integer getPackageWidthCm() {
		return packageWidthCm;
	}

	public void setPackageWidthCm(Integer packageWidthCm) {
		this.packageWidthCm = packageWidthCm;
	}

	public Integer getPackageHeightCm() {
		return packageHeightCm;
	}

	public void setPackageHeightCm(Integer packageHeightCm) {
		this.packageHeightCm = packageHeightCm;
	}

	public Integer getPackageLengthCm() {
		return packageLengthCm;
	}

	public void setPackageLengthCm(Integer packageLengthCm) {
		this.packageLengthCm = packageLengthCm;
	}
}
