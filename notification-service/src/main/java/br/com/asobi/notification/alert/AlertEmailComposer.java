package br.com.asobi.notification.alert;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.notification.config.NotificationProperties;
import br.com.asobi.notification.email.EmailMessage;
import br.com.asobi.notification.subscription.AlertType;
import br.com.asobi.notification.subscription.Subscription;

/** Monta o e-mail de aviso (assunto + HTML do template email/product-alert.html). */
@Component
public class AlertEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;
	private final NotificationProperties properties;

	public AlertEmailComposer(ITemplateEngine templateEngine, NotificationProperties properties) {
		this.templateEngine = templateEngine;
		this.properties = properties;
	}

	public EmailMessage compose(ProductEvent event, Subscription subscription) {
		String unsubscribeUrl = properties.publicUrl() + "/descadastro/" + subscription.getUnsubscribeToken();
		Copy copy = copy(event);

		Context context = new Context(PT_BR);
		context.setVariable("heading", copy.heading());
		context.setVariable("intro", copy.intro());
		context.setVariable("productName", event.productName());
		context.setVariable("price", money(event.price()));
		boolean showOriginal = event.originalPrice() != null && event.originalPrice().compareTo(event.price()) > 0;
		context.setVariable("originalPrice", showOriginal ? money(event.originalPrice()) : null);
		context.setVariable("stockNote", event.type() == AlertType.LOW_STOCK && event.stock() != null
				? (event.stock() == 1 ? "Resta só 1 unidade" : "Restam só " + event.stock() + " unidades")
				: null);
		context.setVariable("productUrl", event.productUrl());
		context.setVariable("reason", copy.reason());
		context.setVariable("unsubscribeUrl", unsubscribeUrl);

		String subject = event.productName() + copy.subjectSuffix() + " — ASOBI";
		return new EmailMessage(subscription.getEmail(), subject,
				templateEngine.process("email/product-alert", context), unsubscribeUrl);
	}

	private static Copy copy(ProductEvent event) {
		return switch (event.type()) {
			case PROMOTION -> new Copy("Entrou em promoção! 🎉",
					"Boa notícia: o jogo que você estava de olho está com preço especial.",
					"Você recebeu este e-mail porque pediu para ser avisado quando este jogo entrasse em promoção.",
					" entrou em promoção");
			case BACK_IN_STOCK -> new Copy("Voltou ao estoque! 🎲",
					"Boa notícia: o jogo que você estava esperando chegou de novo.",
					"Você recebeu este e-mail porque pediu para ser avisado quando este jogo voltasse ao estoque.",
					" voltou ao estoque");
			case LOW_STOCK -> new Copy("Está acabando! ⏳",
					"O jogo que você estava de olho está nas últimas unidades. Se ainda quiser, é melhor garantir o seu.",
					"Você recebeu este e-mail porque pediu para ser avisado quando este jogo estivesse acabando.",
					": últimas unidades");
		};
	}

	private record Copy(String heading, String intro, String reason, String subjectSuffix) {
	}

	private static String money(BigDecimal value) {
		return "R$ " + String.format(PT_BR, "%,.2f", value);
	}
}
