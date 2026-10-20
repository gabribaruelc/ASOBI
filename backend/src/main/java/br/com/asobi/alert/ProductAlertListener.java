package br.com.asobi.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.asobi.alert.ProductAlertGateway.Alert;
import br.com.asobi.catalog.event.ProductEvent;

/**
 * Repassa ao notification-service o que mudou no catálogo. Roda só depois do commit: ninguém
 * recebe aviso de uma alteração que foi desfeita. Uma falha aqui nunca derruba o que o
 * lojista salvou no painel, só vai para o log.
 *
 * A chamada é síncrona de propósito: no Cloud Run a CPU é cortada assim que a resposta sai,
 * então uma tarefa em segundo plano poderia nunca rodar.
 */
@Component
public class ProductAlertListener {

	private static final Logger log = LoggerFactory.getLogger(ProductAlertListener.class);

	private final ProductAlertGateway gateway;
	private final String storeUrl;

	public ProductAlertListener(ProductAlertGateway gateway, @Value("${asobi.store-url}") String storeUrl) {
		this.gateway = gateway;
		this.storeUrl = storeUrl.replaceAll("/+$", "");
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onProductEvent(ProductEvent event) {
		if (!gateway.isEnabled()) {
			return;
		}
		try {
			switch (event.type()) {
				case SALE_STARTED -> gateway.publish(alert(AlertType.PROMOTION, event, null));
				case BACK_IN_STOCK -> gateway.publish(alert(AlertType.BACK_IN_STOCK, event, null));
				case LOW_STOCK -> {
					// esgotou de uma vez: "está acabando" não faz mais sentido
					if (event.stock() > 0) {
						gateway.publish(alert(AlertType.LOW_STOCK, event, event.stock()));
					}
				}
				case DELETED -> gateway.removeProduct(event.productId());
			}
		} catch (RuntimeException ex) {
			log.error("Falha ao avisar o notification-service ({}) do produto {}", event.type(),
					event.productId(), ex);
		}
	}

	private Alert alert(AlertType type, ProductEvent event, Integer stock) {
		return new Alert(type, event.productId(), event.name(), storeUrl + "/jogos/" + event.slug(),
				event.price(), event.originalPrice(), stock);
	}
}
