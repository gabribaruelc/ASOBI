package br.com.asobi.order.event;

import br.com.asobi.order.model.Order;

/**
 * Algo aconteceu com um pedido. Publicado pelo OrderService dentro da transação;
 * quem escuta (ex.: e-mails) decide o que fazer, sem o pedido depender disso.
 */
public record OrderEvent(Type type, Order order) {

	public enum Type {
		/** Pedido criado, aguardando pagamento. */
		PLACED,
		/** Pagamento aprovado (pelo Mercado Pago ou marcado no painel). */
		PAID,
		/** Marcado como enviado no painel. */
		SHIPPED
	}
}
