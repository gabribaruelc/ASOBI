package br.com.asobi.newsletter.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.newsletter.dto.NewsletterCampaignForm;
import br.com.asobi.newsletter.dto.NewsletterProductCard;
import br.com.asobi.newsletter.model.NewsletterCampaign;
import br.com.asobi.newsletter.model.NewsletterDelivery;
import br.com.asobi.newsletter.model.NewsletterSubscriber;
import br.com.asobi.newsletter.repository.NewsletterCampaignRepository;
import br.com.asobi.newsletter.repository.NewsletterDeliveryRepository;
import br.com.asobi.newsletter.repository.NewsletterSubscriberRepository;
import br.com.asobi.notification.EmailMessage;
import br.com.asobi.notification.EmailSender;

/**
 * Newsletters escritas no painel. O envio respeita um limite por 24 horas, porque o plano
 * gratuito do Resend tem 100 e-mails por dia somando os de pedido; o que faltar, o lojista
 * continua depois, sem repetir quem já recebeu.
 *
 * O envio não usa @Transactional: enquanto espera o Resend responder, não pode ficar
 * segurando uma conexão do banco (o plano grátis do Supabase tem poucas).
 */
@Service
public class NewsletterCampaignService {

	private static final Logger log = LoggerFactory.getLogger(NewsletterCampaignService.class);

	private static final Duration QUOTA_WINDOW = Duration.ofDays(1);

	private final NewsletterCampaignRepository campaignRepository;
	private final NewsletterSubscriberRepository subscriberRepository;
	private final NewsletterDeliveryRepository deliveryRepository;
	private final ProductRepository productRepository;
	private final NewsletterEmailComposer composer;
	private final EmailSender emailSender;
	private final Clock clock;
	private final int dailyLimit;

	public NewsletterCampaignService(NewsletterCampaignRepository campaignRepository,
			NewsletterSubscriberRepository subscriberRepository, NewsletterDeliveryRepository deliveryRepository,
			ProductRepository productRepository, NewsletterEmailComposer composer, EmailSender emailSender,
			Clock clock,
			@Value("${asobi.newsletter.daily-limit}") int dailyLimit) {
		this.campaignRepository = campaignRepository;
		this.subscriberRepository = subscriberRepository;
		this.deliveryRepository = deliveryRepository;
		this.productRepository = productRepository;
		this.composer = composer;
		this.emailSender = emailSender;
		this.clock = clock;
		this.dailyLimit = dailyLimit;
	}

	public List<CampaignStatus> list() {
		return campaignRepository.findAllByOrderByCreatedAtDesc().stream().map(this::status).toList();
	}

	public CampaignStatus get(Long id) {
		return status(find(id));
	}

	public NewsletterCampaignForm toForm(NewsletterCampaign campaign) {
		NewsletterCampaignForm form = new NewsletterCampaignForm();
		form.setSubject(campaign.getSubject());
		form.setBody(campaign.getBody());
		form.setButtonLabel(campaign.getButtonLabel());
		form.setButtonUrl(campaign.getButtonUrl());
		form.setProductIds(new ArrayList<>(campaign.getProductIds()));
		return form;
	}

	/** Jogos que podem entrar como destaque: os que têm estoque, os em promoção primeiro. */
	public List<Product> productOptions() {
		return productRepository.findAllByOrderByNameAsc().stream()
				.filter(Product::isInStock)
				.sorted(Comparator.comparing((Product product) -> !product.isOnSale()))
				.toList();
	}

	public List<Product> productsOnSale() {
		return productRepository.findByOriginalPriceIsNotNullAndStockGreaterThanOrderByNameAsc(0);
	}

	/** Rascunho pronto com os jogos em promoção; o lojista revisa o texto antes de enviar. */
	public NewsletterCampaign createPromotion(String adminEmail) {
		List<Product> onSale = productsOnSale();
		if (onSale.isEmpty()) {
			throw new BusinessException("Nenhum jogo com estoque está em promoção agora.");
		}
		NewsletterCampaign campaign = new NewsletterCampaign(adminEmail);
		campaign.update(onSale.size() == 1 ? onSale.get(0).getName() + " está em promoção! 🎉"
				: "Promoções na ASOBI 🎉",
				"Oi! Separamos jogos com preço especial para a próxima brincadeira em família.\n\n"
						+ "As promoções valem enquanto durar o estoque.",
				"Ver todas as promoções", composer.storeUrl() + "/promocoes",
				onSale.stream().limit(12).map(Product::getId).toList());
		return campaignRepository.save(campaign);
	}

	public NewsletterCampaign create(NewsletterCampaignForm form, String adminEmail) {
		NewsletterCampaign campaign = new NewsletterCampaign(adminEmail);
		apply(campaign, form);
		return campaignRepository.save(campaign);
	}

	public void update(Long id, NewsletterCampaignForm form) {
		NewsletterCampaign campaign = findDraft(id);
		apply(campaign, form);
		campaignRepository.save(campaign);
	}

	public void delete(Long id) {
		campaignRepository.delete(findDraft(id));
	}

	public String preview(Long id) {
		NewsletterCampaign campaign = find(id);
		return composer.preview(campaign, composer.cards(campaign.getProductIds()));
	}

	/** Manda a newsletter só para o admin conferir; não conta como envio. */
	public void sendTest(Long id, String adminEmail) {
		NewsletterCampaign campaign = find(id);
		EmailMessage message = composer.compose(campaign, composer.cards(campaign.getProductIds()), adminEmail,
				"exemplo");
		emailSender.send(new EmailMessage(message.to(), "[Teste] " + message.subject(), message.html(),
				message.unsubscribeUrl()));
	}

	/** Começa ou continua o envio, até acabar a lista ou o limite das últimas 24 horas. */
	public SendResult send(Long id) {
		NewsletterCampaign campaign = find(id);
		Instant now = clock.instant();
		if (!campaign.isStarted()) {
			campaign.markStarted(now);
			campaign = campaignRepository.save(campaign);
		}

		int quota = remainingQuota(now);
		List<NewsletterProductCard> cards = composer.cards(campaign.getProductIds());
		int sent = 0;
		while (quota > 0) {
			List<NewsletterSubscriber> batch = subscriberRepository.findPending(campaign.getId(),
					campaign.getStartedAt(), PageRequest.of(0, Math.min(quota, EmailSender.MAX_BATCH_SIZE)));
			if (batch.isEmpty()) {
				break;
			}
			ensureUnsubscribeTokens(batch);

			// Reserva antes de enviar: um segundo clique em "Enviar" bate na chave única e não
			// manda o mesmo e-mail duas vezes.
			List<NewsletterDelivery> reserved;
			try {
				Long campaignId = campaign.getId();
				reserved = deliveryRepository.saveAll(batch.stream()
						.map(subscriber -> new NewsletterDelivery(campaignId, subscriber.getId(), now))
						.toList());
			} catch (DataIntegrityViolationException ex) {
				return result(campaign, sent, SendOutcome.ALREADY_RUNNING);
			}

			NewsletterCampaign current = campaign;
			try {
				emailSender.sendAll(batch.stream()
						.map(subscriber -> composer.compose(current, cards, subscriber.getEmail(),
								subscriber.getUnsubscribeToken()))
						.toList());
			} catch (RuntimeException ex) {
				log.error("Falha ao enviar a newsletter #{} para {} inscrito(s)", campaign.getId(), batch.size(), ex);
				deliveryRepository.deleteAllInBatch(reserved);
				return result(campaign, sent, SendOutcome.FAILED);
			}
			sent += batch.size();
			quota -= batch.size();
		}
		long pending = pending(campaign);
		return result(campaign, sent, pending == 0 ? SendOutcome.DONE : SendOutcome.DAILY_LIMIT);
	}

	/** Quantos e-mails de newsletter ainda cabem nas próximas horas. */
	public int remainingQuota() {
		return remainingQuota(clock.instant());
	}

	public int dailyLimit() {
		return dailyLimit;
	}

	private int remainingQuota(Instant now) {
		long used = deliveryRepository.countBySentAtAfter(now.minus(QUOTA_WINDOW));
		return (int) Math.max(0, dailyLimit - used);
	}

	private void ensureUnsubscribeTokens(List<NewsletterSubscriber> subscribers) {
		List<NewsletterSubscriber> missing = subscribers.stream()
				.filter(subscriber -> subscriber.getUnsubscribeToken() == null)
				.toList();
		missing.forEach(subscriber -> subscriber.assignUnsubscribeToken(NewsletterService.newUnsubscribeToken()));
		subscriberRepository.saveAll(missing);
	}

	/** Rascunho vai para todos os inscritos de agora; depois do início, só para quem já estava. */
	private long pending(NewsletterCampaign campaign) {
		Instant until = campaign.isStarted() ? campaign.getStartedAt() : clock.instant();
		return subscriberRepository.countPending(campaign.getId(), until);
	}

	private CampaignStatus status(NewsletterCampaign campaign) {
		return new CampaignStatus(campaign, deliveryRepository.countByCampaignId(campaign.getId()), pending(campaign));
	}

	private SendResult result(NewsletterCampaign campaign, int sent, SendOutcome outcome) {
		return new SendResult(sent, pending(campaign), outcome);
	}

	private static void apply(NewsletterCampaign campaign, NewsletterCampaignForm form) {
		campaign.update(form.getSubject().strip(), form.getBody().strip(), blankToNull(form.getButtonLabel()),
				blankToNull(form.getButtonUrl()), form.getProductIds());
	}

	private static String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.strip() : null;
	}

	private NewsletterCampaign find(Long id) {
		return campaignRepository.findById(id).orElseThrow(() -> new NotFoundException("Newsletter não encontrada."));
	}

	private NewsletterCampaign findDraft(Long id) {
		NewsletterCampaign campaign = find(id);
		if (campaign.isStarted()) {
			throw new BusinessException("Esta newsletter já começou a ser enviada e não pode mais ser alterada.");
		}
		return campaign;
	}

	/**
	 * @param sent    quantos já receberam
	 * @param pending quantos ainda faltam
	 */
	public record CampaignStatus(NewsletterCampaign campaign, long sent, long pending) {

		public boolean isDone() {
			return campaign.isStarted() && pending == 0;
		}
	}

	public enum SendOutcome {
		DONE, DAILY_LIMIT, FAILED, ALREADY_RUNNING
	}

	/**
	 * @param sent    enviados agora
	 * @param pending quantos ainda faltam
	 */
	public record SendResult(int sent, long pending, SendOutcome outcome) {
	}
}
