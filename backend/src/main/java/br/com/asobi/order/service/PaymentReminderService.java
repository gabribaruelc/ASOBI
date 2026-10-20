package br.com.asobi.order.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.order.event.OrderEvent;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderItem;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.repository.OrderRepository;

/**
 * Lembrete único para quem fez o pedido e não chegou a pagar ("carrinho abandonado"). Roda quando
 * o Cloud Scheduler chama {@code POST /api/jobs/payment-reminders}: no Cloud Run não há processo
 * rodando o tempo todo para agendar sozinho.
 */
@Service
@Transactional
public class PaymentReminderService {

	private static final Logger log = LoggerFactory.getLogger(PaymentReminderService.class);

	private final OrderRepository orderRepository;
	private final OrderService orderService;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final Duration delay;
	private final Duration maxAge;

	public PaymentReminderService(OrderRepository orderRepository, OrderService orderService,
			ApplicationEventPublisher events, Clock clock,
			@Value("${asobi.payment-reminders.delay}") Duration delay,
			@Value("${asobi.payment-reminders.max-age}") Duration maxAge) {
		this.orderRepository = orderRepository;
		this.orderService = orderService;
		this.events = events;
		this.clock = clock;
		this.delay = delay;
		this.maxAge = maxAge;
	}

	/**
	 * Lembra quem fez o pedido entre {@code maxAge} e {@code delay} atrás. Pula o pedido que não dá
	 * mais para pagar (sem Mercado Pago ou com algum jogo esgotado); ele pode entrar numa próxima
	 * rodada se o estoque voltar dentro do prazo.
	 *
	 * @return quantos lembretes saíram
	 */
	public int sendDue() {
		Instant now = clock.instant();
		List<Order> candidates = orderRepository.findPaymentReminderCandidates(OrderStatus.PENDING_PAYMENT,
				now.minus(maxAge), now.minus(delay));
		int sent = 0;
		for (Order order : candidates) {
			if (!orderService.isPayable(order) || !hasStock(order)) {
				continue;
			}
			order.markPaymentReminderSent(now);
			events.publishEvent(new OrderEvent(OrderEvent.Type.PAYMENT_REMINDER, order));
			sent++;
		}
		if (sent > 0) {
			log.info("{} lembrete(s) de pagamento enviados", sent);
		}
		return sent;
	}

	private static boolean hasStock(Order order) {
		for (OrderItem item : order.getItems()) {
			if (item.getProduct() == null || item.getProduct().getStock() < item.getQuantity()) {
				return false;
			}
		}
		return true;
	}
}
