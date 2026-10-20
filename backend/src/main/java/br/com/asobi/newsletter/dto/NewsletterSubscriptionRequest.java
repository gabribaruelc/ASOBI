package br.com.asobi.newsletter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Só o e-mail do adulto: nada de nome ou dado de criança (LGPD). */
public record NewsletterSubscriptionRequest(
		@NotBlank(message = "Informe seu e-mail.")
		@Email(message = "E-mail inválido.")
		@Size(max = 254, message = "E-mail muito longo.")
		String email) {
}
