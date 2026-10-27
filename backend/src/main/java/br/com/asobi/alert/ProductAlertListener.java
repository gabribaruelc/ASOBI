package br.com.asobi.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.asobi.alert.ProductAlertGateway.Alert;
import br.com.asobi.catalog.event.ProductEvent;

/**
 * Conta ao notification-service o que mudou num jogo. Só roda depois que a mudança foi salva
 * no banco, para ninguém receber aviso de algo que foi desfeito. Se der erro aqui, o que o
 * lojista salvou no painel continua salvo; o erro só vai para o log.
 *
 * A chamada espera a resposta (não roda em segundo plano) de propósito: o Cloud Run corta o
 * processador assim que a resposta sai, então uma tarefa em segundo plano poderia nem rodar.
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
