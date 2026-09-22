package br.com.asobi.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido enviado pela loja. Só slug e quantidade dos itens: preço e total
 * são calculados no servidor a partir do banco — nunca confiamos no navegador.
 */
public record OrderRequest(
		@NotNull(message = "Informe seus dados.") @Valid CustomerRequest customer,
		@NotNull(message = "Informe o endereço de entrega.") @Valid AddressRequest shippingAddress,
		@NotEmpty(message = "O carrinho está vazio.")
		@Size(max = 50, message = "Itens demais no carrinho.")
		List<@Valid ItemRequest> items) {

	public record CustomerRequest(
			@NotBlank(message = "Informe seu nome completo.")
			@Size(max = 120, message = "Nome muito longo.")
			String name,

			@NotBlank(message = "Informe seu e-mail.")
			@Email(message = "E-mail inválido.")
			@Size(max = 254, message = "E-mail muito longo.")
			String email,

			@NotBlank(message = "Informe um telefone/WhatsApp.")
			@Pattern(regexp = "^[0-9()+\\-\\s]{10,20}$", message = "Telefone inválido.")
			String phone) {
	}

	public record AddressRequest(
			@NotBlank(message = "Informe o CEP.")
			@Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido.")
			String postalCode,

			@NotBlank(message = "Informe a rua.")
			@Size(max = 200, message = "Endereço muito longo.")
			String street,

			@NotBlank(message = "Informe o número.")
			@Size(max = 20, message = "Número muito longo.")
			String number,

			@Size(max = 100, message = "Complemento muito longo.")
			String complement,

			@NotBlank(message = "Informe o bairro.")
			@Size(max = 100, message = "Bairro muito longo.")
			String district,

			@NotBlank(message = "Informe a cidade.")
			@Size(max = 100, message = "Cidade muito longa.")
			String city,

			@NotBlank(message = "Informe o estado (UF).")
			@Pattern(regexp = "^[A-Za-z]{2}$", message = "UF inválida.")
			String state) {
	}

	public record ItemRequest(
			@NotBlank(message = "Produto inválido.")
			String slug,

			@NotNull(message = "Informe a quantidade.")
			@Min(value = 1, message = "Quantidade mínima é 1.")
			@Max(value = 20, message = "Quantidade máxima é 20 por produto.")
			Integer quantity) {
	}
}
