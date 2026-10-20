package br.com.asobi.newsletter.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.catalog.service.ProductImageService;
import br.com.asobi.newsletter.dto.NewsletterProductCard;
import br.com.asobi.newsletter.model.NewsletterCampaign;
import br.com.asobi.notification.EmailMessage;

/** Monta a newsletter (template email/newsletter.html) com o link de descadastro de cada inscrito. */
@Component
public class NewsletterEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;
	private final ProductRepository productRepository;
	private final ProductImageService imageService;
	private final String storeUrl;
	private final String publicUrl;

	public NewsletterEmailComposer(ITemplateEngine templateEngine, ProductRepository productRepository,
			ProductImageService imageService,
			@Value("${asobi.store-url}") String storeUrl,
			@Value("${asobi.public-url}") String publicUrl) {
		this.templateEngine = templateEngine;
		this.productRepository = productRepository;
		this.imageService = imageService;
		this.storeUrl = storeUrl.replaceAll("/+$", "");
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	/**
	 * Os jogos em destaque com o preço de agora. Jogo esgotado fica de fora: não adianta anunciar.
	 * Montado uma vez por envio, não por inscrito.
	 */
	@Transactional(readOnly = true)
	public List<NewsletterProductCard> cards(Collection<Long> productIds) {
		if (productIds.isEmpty()) {
			return List.of();
		}
		return productRepository.findAllById(productIds).stream()
				.filter(Product::isInStock)
				.sorted(Comparator.comparing(Product::getName))
				.map(this::card)
				.toList();
	}

	public EmailMessage compose(NewsletterCampaign campaign, List<NewsletterProductCard> cards, String to,
			String unsubscribeToken) {
		String unsubscribeUrl = unsubscribeUrl(unsubscribeToken);
		return new EmailMessage(List.of(to), campaign.getSubject(), html(campaign, cards, unsubscribeUrl),
				unsubscribeUrl);
	}

	/** O link de descadastro da prévia e do teste não cancela ninguém. */
	public String preview(NewsletterCampaign campaign, List<NewsletterProductCard> cards) {
		return html(campaign, cards, unsubscribeUrl("exemplo"));
	}

	public String unsubscribeUrl(String token) {
		return publicUrl + "/newsletter/descadastro/" + token;
	}

	public String storeUrl() {
		return storeUrl;
	}

	private NewsletterProductCard card(Product product) {
		List<String> images = imageService.urls(product);
		return new NewsletterProductCard(product.getId(), product.getName(), storeUrl + "/jogos/" + product.getSlug(),
				images.isEmpty() ? null : images.get(0), product.getIcon(), product.getPrice(),
				product.isOnSale() ? product.getOriginalPrice() : null);
	}

	private String html(NewsletterCampaign campaign, List<NewsletterProductCard> cards, String unsubscribeUrl) {
		Context context = new Context(PT_BR);
		context.setVariable("subject", campaign.getSubject());
		// parágrafos separados por linha em branco, como nas políticas
		context.setVariable("paragraphs", Arrays.stream(campaign.getBody().strip().split("\\R\\s*\\R"))
				.map(String::strip)
				.filter(StringUtils::hasText)
				.toList());
		context.setVariable("cards", cards);
		context.setVariable("buttonLabel", StringUtils.hasText(campaign.getButtonLabel())
				? campaign.getButtonLabel() : "Ver os jogos na loja");
		context.setVariable("buttonUrl", StringUtils.hasText(campaign.getButtonUrl())
				? campaign.getButtonUrl() : storeUrl);
		context.setVariable("unsubscribeUrl", unsubscribeUrl);
		return templateEngine.process("email/newsletter", context);
	}
}
