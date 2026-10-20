package br.com.asobi.alert;

import java.math.BigDecimal;

/**
 * Avisos de "entrou em promoção", "voltou ao estoque" e "está acabando". As inscrições e o envio dos e-mails
 * ficam no notification-service; o backend só repassa o pedido da loja e conta o que mudou.
 */
public interface ProductAlertGateway {

	/** Sem o serviço configurado, a loja não oferece o "avise-me". */
	boolean isEnabled();

	/**
	 * Registra o pedido de aviso, já com o consentimento dado na loja.
	 *
	 * @throws br.com.asobi.common.exception.BusinessException com mensagem pronta para a loja
	 */
	void subscribe(String email, Long productId, AlertType type);

	/** @throws RuntimeException se o serviço não respondeu; quem chama decide o que fazer */
	void publish(Alert alert);

	/** O produto saiu do catálogo: os pedidos de aviso dele são apagados. */
	void removeProduct(Long productId);

	/**
	 * @param price         valor cobrado agora
	 * @param originalPrice preço cheio, para o "de/por" do aviso de promoção; pode ser null
	 * @param stock         unidades restantes, só no aviso de "está acabando"; nos outros, null
	 */
	record Alert(AlertType type, Long productId, String productName, String productUrl, BigDecimal price,
			BigDecimal originalPrice, Integer stock) {
	}
}
