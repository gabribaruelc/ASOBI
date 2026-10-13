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
		boolean promotion = event.type() == AlertType.PROMOTION;
		String unsubscribeUrl = properties.publicUrl() + "/descadastro/" + subscription.getUnsubscribeToken();

		Context context = new Context(PT_BR);
		context.setVariable("heading", promotion ? "Entrou em promoção! 🎉" : "Voltou ao estoque! 🎲");
		context.setVariable("intro", promotion
				? "Boa notícia: o jogo que você estava de olho está com preço especial."
				: "Boa notícia: o jogo que você estava esperando chegou de novo.");
		context.setVariable("productName", event.productName());
		context.setVariable("price", money(event.price()));
		boolean showOriginal = promotion && event.originalPrice() != null
				&& event.originalPrice().compareTo(event.price()) > 0;
		context.setVariable("originalPrice", showOriginal ? money(event.originalPrice()) : null);
		context.setVariable("productUrl", event.productUrl());
		context.setVariable("reason", promotion
				? "Você recebeu este e-mail porque pediu para ser avisado quando este jogo entrasse em promoção."
				: "Você recebeu este e-mail porque pediu para ser avisado quando este jogo voltasse ao estoque.");
		context.setVariable("unsubscribeUrl", unsubscribeUrl);

		String subject = event.productName() + (promotion ? " entrou em promoção" : " voltou ao estoque") + " — ASOBI";
		return new EmailMessage(subscription.getEmail(), subject,
				templateEngine.process("email/product-alert", context), unsubscribeUrl);
	}

	private static String money(BigDecimal value) {
		return "R$ " + String.format(PT_BR, "%,.2f", value);
	}
}
