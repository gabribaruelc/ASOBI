package br.com.asobi.notification.alert;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.asobi.notification.config.NotificationProperties;
import br.com.asobi.notification.email.EmailMessage;
import br.com.asobi.notification.email.EmailSender;
import br.com.asobi.notification.subscription.Subscription;
import br.com.asobi.notification.subscription.SubscriptionRepository;

@Service
public class ProductAlertService {

	private static final Logger log = LoggerFactory.getLogger(ProductAlertService.class);

	private final SubscriptionRepository subscriptionRepository;
	private final AlertEmailComposer composer;
	private final EmailSender emailSender;
	private final NotificationProperties properties;
	private final Clock clock;

	public ProductAlertService(SubscriptionRepository subscriptionRepository, AlertEmailComposer composer,
			EmailSender emailSender, NotificationProperties properties, Clock clock) {
		this.subscriptionRepository = subscriptionRepository;
		this.composer = composer;
		this.emailSender = emailSender;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Avisa quem pediu esse tipo de aviso para o jogo. Não usa @Transactional de propósito:
	 * enquanto espera o Resend, não pode ficar segurando uma conexão do banco. Quem recebeu é
	 * marcado logo depois de cada grupo, então mandar o mesmo evento de novo não repete o e-mail.
	 */
	public AlertResult notifySubscribers(ProductEvent event) {
		Instant now = clock.instant();
		List<Subscription> subscribers = subscriptionRepository.findByProductIdAndType(event.productId(),
				event.type());
		List<Subscription> due = subscribers.stream()
				.filter(subscription -> subscription.isDue(now, properties.cooldown()))
				.toList();

		int sent = 0;
		int failed = 0;
		for (int from = 0; from < due.size(); from += EmailSender.MAX_BATCH_SIZE) {
			List<Subscription> batch = due.subList(from, Math.min(from + EmailSender.MAX_BATCH_SIZE, due.size()));
			List<EmailMessage> messages = batch.stream().map(subscription -> composer.compose(event, subscription))
					.toList();
			try {
				emailSender.sendAll(messages);
				subscriptionRepository.markNotified(batch.stream().map(Subscription::getId).toList(), now);
				sent += batch.size();
			} catch (RuntimeException ex) {
				log.error("Falha ao enviar {} aviso(s) {} do produto {}", batch.size(), event.type(),
						event.productId(), ex);
				failed += batch.size();
			}
		}
		return new AlertResult(subscribers.size(), sent, failed, subscribers.size() - due.size());
	}

	/**
	 * @param subscribers quem pediu este aviso para o produto
	 * @param skipped     quem já tinha recebido o mesmo aviso há pouco tempo
	 */
	public record AlertResult(int subscribers, int sent, int failed, int skipped) {
	}
}
