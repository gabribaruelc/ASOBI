package br.com.asobi.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.asobi.admin.directory.AdminAccount;
import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.catalog.event.ProductEvent;

/** Avisa os admins quando uma venda deixa um jogo com estoque baixo. O envio é depois do commit. */
@Component
public class StockEmailListener {

	private static final Logger log = LoggerFactory.getLogger(StockEmailListener.class);

	private final StockEmailComposer composer;
	private final EmailDispatcher dispatcher;
	private final AdminDirectory adminDirectory;

	public StockEmailListener(StockEmailComposer composer, EmailDispatcher dispatcher,
			AdminDirectory adminDirectory) {
		this.composer = composer;
		this.dispatcher = dispatcher;
		this.adminDirectory = adminDirectory;
	}

	@EventListener
	public void onProductEvent(ProductEvent event) {
		if (event.type() != ProductEvent.Type.LOW_STOCK) {
			return;
		}
		try {
			List<String> admins = adminDirectory.listAdmins().stream().map(AdminAccount::email).toList();
			if (!admins.isEmpty()) {
				dispatcher.sendAfterCommit(composer.lowStock(event, admins));
			}
		} catch (RuntimeException ex) {
			// um problema no aviso não pode impedir a venda
			log.error("Falha ao preparar o aviso de estoque baixo do produto {}", event.productId(), ex);
		}
	}
}
