package br.com.asobi.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.asobi.admin.model.AdminUser;
import br.com.asobi.admin.service.AdminUserService;
import br.com.asobi.order.event.OrderEvent;
import br.com.asobi.order.model.Order;

/**
 * E-mails transacionais do pedido. Roda dentro da transação do pedido (para ler itens e
 * admins), mas o envio em si só acontece depois do commit — ver {@link EmailDispatcher}.
 */
@Component
public class OrderEmailListener {

	private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);

	private final OrderEmailComposer composer;
	private final EmailDispatcher dispatcher;
	private final AdminUserService adminUserService;

	public OrderEmailListener(OrderEmailComposer composer, EmailDispatcher dispatcher,
			AdminUserService adminUserService) {
		this.composer = composer;
		this.dispatcher = dispatcher;
		this.adminUserService = adminUserService;
	}

	@EventListener
	public void onOrderEvent(OrderEvent event) {
		Order order = event.order();
		try {
			switch (event.type()) {
				case PLACED -> dispatcher.sendAfterCommit(composer.placed(order));
				case PAID -> {
					dispatcher.sendAfterCommit(composer.paid(order));
					List<String> admins = adminUserService.listAdmins().stream().map(AdminUser::getEmail).toList();
					if (!admins.isEmpty()) {
						dispatcher.sendAfterCommit(composer.paidAdminNotice(order, admins));
					}
				}
				case SHIPPED -> dispatcher.sendAfterCommit(composer.shipped(order));
			}
		} catch (RuntimeException ex) {
			// Um problema no e-mail não pode impedir a venda.
			log.error("Falha ao preparar o e-mail ({}) do pedido #{}", event.type(), order.getId(), ex);
		}
	}
}
