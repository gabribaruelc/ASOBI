package br.com.asobi.notification.alert;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.notification.alert.ProductAlertService.AlertResult;

@RestController
@RequestMapping("/api/product-events")
public class ProductEventController {

	private final ProductAlertService productAlertService;

	public ProductEventController(ProductAlertService productAlertService) {
		this.productAlertService = productAlertService;
	}

	/** O backend avisa que um produto entrou em promoção ou voltou ao estoque. */
	@PostMapping
	public AlertResult publish(@Validated @RequestBody ProductEvent event) {
		return productAlertService.notifySubscribers(event);
	}
}
