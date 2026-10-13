package br.com.asobi.alert;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Só o e-mail do adulto: nada de nome ou dado de criança (LGPD).
 *
 * @param consent a caixa "quero receber este aviso" da loja, que começa desmarcada
 */
public record ProductAlertRequest(
		@NotBlank(message = "Informe seu e-mail.")
		@Email(message = "E-mail inválido.")
		@Size(max = 254, message = "E-mail muito longo.")
		String email,

		@NotBlank(message = "Informe o produto.")
		String productSlug,

		@NotNull(message = "Informe o tipo de aviso.")
		AlertType type,

		@AssertTrue(message = "Marque a caixa para autorizar o envio do aviso.")
		boolean consent) {
}
