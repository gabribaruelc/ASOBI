package br.com.asobi.shipping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
public class ShippingController {

	private final ShippingService shippingService;

	public ShippingController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@PostMapping("/api/shipping/quote")
	public List<ShippingOption> quote(@Validated @RequestBody QuoteRequest request) {
		Map<String, Integer> quantities = new LinkedHashMap<>();
		request.items().forEach(item -> quantities.merge(item.slug(), item.quantity(), Integer::sum));
		return shippingService.quote(request.postalCode(), quantities);
	}

	public record QuoteRequest(
			@NotBlank(message = "Informe o CEP.")
			@Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido.")
			String postalCode,

			@NotEmpty(message = "O carrinho está vazio.")
			@Size(max = 50, message = "Itens demais no carrinho.")
			List<@Valid CartItem> items) {
	}

	/** Item do carrinho: só slug e quantidade (preço e medidas vêm do servidor). */
	public record CartItem(
			@NotBlank(message = "Produto inválido.")
			String slug,

			@NotNull(message = "Informe a quantidade.")
			@Min(value = 1, message = "Quantidade mínima é 1.")
			@Max(value = 20, message = "Quantidade máxima é 20 por produto.")
			Integer quantity) {
	}
}
