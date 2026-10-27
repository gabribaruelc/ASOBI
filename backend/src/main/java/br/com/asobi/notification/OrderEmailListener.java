package br.com.asobi.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.asobi.admin.directory.AdminAccount;
import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.order.event.OrderEvent;
import br.com.asobi.order.model.Order;

/**
 * E-mails do pedido (recebido, pago, enviado). Monta o e-mail junto com a gravação do pedido
 * (para conseguir ler os itens e os admins), mas só envia depois que tudo foi salvo no banco.
 * Ver {@link EmailDispatcher}.
 */
@Component
public class OrderEmailListener {

	private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);

	private final OrderEmailComposer composer;
	private final EmailDispatcher dispatcher;
	private final AdminDirectory adminDirectory;

	public OrderEmailListener(OrderEmailComposer composer, EmailDispatcher dispatcher,
			AdminDirectory adminDirectory) {
		this.composer = composer;
		this.dispatcher = dispatcher;
		this.adminDirectory = adminDirectory;
	}

	@EventListener
	public void onOrderEvent(OrderEvent event) {
		Order order = event.order();
		try {
			switch (event.type()) {
				case PLACED -> dispatcher.sendAfterCommit(composer.placed(order));
				case PAID -> {
					dispatcher.sendAfterCommit(composer.paid(order));
					List<String> admins = adminDirectory.listAdmins().stream().map(AdminAccount::email).toList();
					if (!admins.isEmpty()) {
						dispatcher.sendAfterCommit(composer.paidAdminNotice(order, admins));
					}
				}
				case SHIPPED -> dispatcher.sendAfterCommit(composer.shipped(order));
				case PAYMENT_REMINDER -> dispatcher.sendAfterCommit(composer.paymentReminder(order));
			}
		} catch (RuntimeException ex) {
			// Um problema no e-mail não pode impedir a venda.
			log.error("Falha ao preparar o e-mail ({}) do pedido #{}", event.type(), order.getId(), ex);
		}
	}
}
