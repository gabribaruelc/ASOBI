package br.com.asobi.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulário de produto do painel. Os preços seguem o jeito que a Donna pensa:
 * "preço normal" + (opcional) "preço promocional". No banco isso vira
 * price (quanto se paga) e original_price (preço riscado, só em promoção).
 */
public class ProductForm {

	@NotBlank(message = "Informe o nome.")
	@Size(max = 150, message = "Máximo de 150 caracteres.")
	private String name;

	@Size(max = 16, message = "Use só um emoji.")
	private String icon;

	@NotBlank(message = "Informe a descrição.")
	@Size(max = 4000, message = "Máximo de 4000 caracteres.")
	private String description;

	@NotBlank(message = "Informe a habilidade estimulada.")
	@Size(max = 100, message = "Máximo de 100 caracteres.")
	private String skill;

	@NotNull(message = "Escolha a faixa etária.")
	private Long categoryId;

	@NotBlank(message = "Informe a idade recomendada (ex.: 6+ anos).")
	@Size(max = 40, message = "Máximo de 40 caracteres.")
	private String ageLabel;

	@NotBlank(message = "Informe o nº de jogadores (ex.: 2 a 4 jogadores).")
	@Size(max = 60, message = "Máximo de 60 caracteres.")
	private String players;

	@NotNull(message = "Informe o preço.")
	@DecimalMin(value = "0.01", message = "O preço precisa ser maior que zero.")
	@Digits(integer = 8, fraction = 2, message = "Use no máximo 2 casas decimais.")
	private BigDecimal regularPrice;

	private boolean onSale;

	@DecimalMin(value = "0.01", message = "O preço precisa ser maior que zero.")
	@Digits(integer = 8, fraction = 2, message = "Use no máximo 2 casas decimais.")
	private BigDecimal salePrice;

	@NotNull(message = "Informe o estoque.")
	@Min(value = 0, message = "O estoque não pode ser negativo.")
	private Integer stock = 0;

	private boolean cooperative;

	/** Último dia em Novidades (vazio = não aparece em Novidades). */
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate newUntil;

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

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
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

	public BigDecimal getRegularPrice() {
		return regularPrice;
	}

	public void setRegularPrice(BigDecimal regularPrice) {
		this.regularPrice = regularPrice;
	}

	public boolean isOnSale() {
		return onSale;
	}

	public void setOnSale(boolean onSale) {
		this.onSale = onSale;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public boolean isCooperative() {
		return cooperative;
	}

	public void setCooperative(boolean cooperative) {
		this.cooperative = cooperative;
	}

	public LocalDate getNewUntil() {
		return newUntil;
	}

	public void setNewUntil(LocalDate newUntil) {
		this.newUntil = newUntil;
	}
}
