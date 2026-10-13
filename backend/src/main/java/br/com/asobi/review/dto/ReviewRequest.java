package br.com.asobi.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sem campo de status (toda avaliação entra pendente) e só com o primeiro nome (LGPD). */
public record ReviewRequest(
		@NotBlank(message = "Informe seu nome.")
		@Size(max = 80, message = "Nome muito longo.")
		String name,

		@NotNull(message = "Escolha uma nota.")
		@Min(value = 1, message = "A nota vai de 1 a 5.")
		@Max(value = 5, message = "A nota vai de 1 a 5.")
		Integer rating,

		@NotBlank(message = "Escreva um comentário.")
		@Size(max = 1000, message = "O comentário pode ter até 1000 caracteres.")
		String comment) {
}
