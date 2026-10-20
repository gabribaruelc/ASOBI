package br.com.asobi.alert;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductAlertController {

	private final ProductAlertService productAlertService;

	public ProductAlertController(ProductAlertService productAlertService) {
		this.productAlertService = productAlertService;
	}

	/** O "avise-me" da ficha do jogo. Pedir duas vezes o mesmo aviso não é erro. */
	@PostMapping("/api/product-alerts")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribe(@Validated @RequestBody ProductAlertRequest request) {
		productAlertService.subscribe(request);
	}
}
