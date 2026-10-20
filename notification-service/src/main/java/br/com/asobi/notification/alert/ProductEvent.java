package br.com.asobi.notification.alert;

import java.math.BigDecimal;

import br.com.asobi.notification.subscription.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * O que mudou num produto, contado pelo backend. Traz tudo o que o e-mail precisa: este
 * serviço não consulta o catálogo.
 *
 * @param price         preço que a pessoa paga agora (o promocional, se estiver em promoção)
 * @param originalPrice preço cheio, para o "de R$ X por R$ Y" da promoção; opcional
 * @param stock         unidades restantes, para o "restam só N" do aviso de estoque baixo; opcional
 */
public record ProductEvent(
		@NotNull(message = "Informe o tipo do evento.")
		AlertType type,

		@NotNull(message = "Informe o produto.")
		Long productId,

		@NotBlank(message = "Informe o nome do produto.")
		@Size(max = 200, message = "Nome longo demais.")
		String productName,

		@NotBlank(message = "Informe o endereço do produto na loja.")
		@Pattern(regexp = "https?://\\S+", message = "Endereço do produto inválido.")
		@Size(max = 500, message = "Endereço longo demais.")
		String productUrl,

		@NotNull(message = "Informe o preço.")
		@PositiveOrZero(message = "Preço inválido.")
		BigDecimal price,

		@PositiveOrZero(message = "Preço inválido.")
		BigDecimal originalPrice,

		@PositiveOrZero(message = "Estoque inválido.")
		Integer stock) {
}
