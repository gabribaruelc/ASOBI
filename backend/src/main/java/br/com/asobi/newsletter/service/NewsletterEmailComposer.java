package br.com.asobi.newsletter.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.newsletter.model.NewsletterCampaign;
import br.com.asobi.notification.EmailMessage;

/** Monta a newsletter (template email/newsletter.html) com o link de descadastro de cada inscrito. */
@Component
public class NewsletterEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;
	private final String storeUrl;
	private final String publicUrl;

	public NewsletterEmailComposer(ITemplateEngine templateEngine,
			@Value("${asobi.store-url}") String storeUrl,
			@Value("${asobi.public-url}") String publicUrl) {
		this.templateEngine = templateEngine;
		this.storeUrl = storeUrl.replaceAll("/+$", "");
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	public EmailMessage compose(NewsletterCampaign campaign, String to, String unsubscribeToken) {
		String unsubscribeUrl = unsubscribeUrl(unsubscribeToken);
		return new EmailMessage(List.of(to), campaign.getSubject(), html(campaign, unsubscribeUrl), unsubscribeUrl);
	}

	/** O link de descadastro da prévia e do teste não cancela ninguém. */
	public String preview(NewsletterCampaign campaign) {
		return html(campaign, unsubscribeUrl("exemplo"));
	}

	public String unsubscribeUrl(String token) {
		return publicUrl + "/newsletter/descadastro/" + token;
	}

	private String html(NewsletterCampaign campaign, String unsubscribeUrl) {
		Context context = new Context(PT_BR);
		context.setVariable("subject", campaign.getSubject());
		// parágrafos separados por linha em branco, como nas políticas
		context.setVariable("paragraphs", Arrays.stream(campaign.getBody().strip().split("\\R\\s*\\R"))
				.map(String::strip)
				.filter(StringUtils::hasText)
				.toList());
		context.setVariable("buttonLabel", StringUtils.hasText(campaign.getButtonLabel())
				? campaign.getButtonLabel() : "Ver os jogos na loja");
		context.setVariable("buttonUrl", StringUtils.hasText(campaign.getButtonUrl())
				? campaign.getButtonUrl() : storeUrl);
		context.setVariable("unsubscribeUrl", unsubscribeUrl);
		return templateEngine.process("email/newsletter", context);
	}
}
