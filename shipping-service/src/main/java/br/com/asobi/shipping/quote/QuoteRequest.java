package br.com.asobi.shipping.quote;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Pedido de cotação: de onde, para onde e o que vai na caixa. */
public record QuoteRequest(
		@NotBlank(message = "Informe o CEP de origem.")
		@Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP de origem inválido.")
		String originPostalCode,

		@NotBlank(message = "Informe o CEP de destino.")
		@Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP de destino inválido.")
		String destinationPostalCode,

		@NotEmpty(message = "Informe ao menos um volume.")
		@Size(max = 50, message = "Volumes demais.")
		List<@Valid @NotNull Parcel> parcels) {

	/** Um produto do carrinho com as medidas da caixa. */
	public record Parcel(
			@NotBlank @Size(max = 100) String id,
			@Min(1) int quantity,
			@NotNull @DecimalMin("0.00") BigDecimal unitValue,
			@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
			@Min(1) int widthCm,
			@Min(1) int heightCm,
			@Min(1) int lengthCm) {
	}
}
