package br.com.asobi.notification;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.catalog.event.ProductEvent;

/** Aviso de estoque baixo para os admins (template email/notice.html). */
@Component
public class StockEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;
	private final String publicUrl;

	public StockEmailComposer(ITemplateEngine templateEngine, @Value("${asobi.public-url}") String publicUrl) {
		this.templateEngine = templateEngine;
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	public EmailMessage lowStock(ProductEvent event, List<String> adminEmails) {
		boolean soldOut = event.stock() == 0;
		Context context = new Context(PT_BR);
		context.setVariable("heading", soldOut ? "Esgotou! 📦" : "Estoque baixo 📦");
		context.setVariable("paragraphs", List.of(soldOut
				? "A última venda zerou o estoque de " + event.name() + ". Na loja, ele aparece como esgotado."
				: event.name() + " está acabando. Se for repor, já dá para pedir ao fornecedor."));
		context.setVariable("highlight", soldOut ? "Estoque: 0" : "Restam " + units(event.stock()));
		context.setVariable("buttonLabel", "Abrir o jogo no painel");
		context.setVariable("buttonUrl", publicUrl + "/admin/produtos/" + event.productId() + "/editar");
		context.setVariable("footerNote",
				"Aviso automático do painel ASOBI. O limite de estoque baixo fica em Configurações.");
		String subject = (soldOut ? "Esgotou: " : "Estoque baixo: ") + event.name()
				+ (soldOut ? "" : " (" + units(event.stock()) + ")");
		return new EmailMessage(adminEmails, subject, templateEngine.process("email/notice", context));
	}

	private static String units(int stock) {
		return stock == 1 ? "1 unidade" : stock + " unidades";
	}
}
