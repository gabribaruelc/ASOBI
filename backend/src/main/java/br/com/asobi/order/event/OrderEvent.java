package br.com.asobi.order.event;

import br.com.asobi.order.model.Order;

/**
 * Avisa que algo aconteceu com um pedido. O OrderService anuncia, e quem está "escutando"
 * (ex.: os e-mails) decide o que fazer. Assim o pedido não depende do módulo de e-mail.
 */
public record OrderEvent(Type type, Order order) {

	public enum Type {
		/** Pedido criado, aguardando pagamento. */
		PLACED,
		/** Pagamento aprovado (pelo Mercado Pago ou marcado no painel). */
		PAID,
		/** Marcado como enviado no painel. */
		SHIPPED,
		/** Ficou sem pagar: lembrete único com o link para pagar. */
		PAYMENT_REMINDER
	}
}
