package br.com.asobi.notification.subscription;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SubscriptionService {

	private final SubscriptionRepository subscriptionRepository;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public SubscriptionService(SubscriptionRepository subscriptionRepository, Clock clock) {
		this.subscriptionRepository = subscriptionRepository;
		this.clock = clock;
	}

	public static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	/** Pedir de novo o mesmo aviso devolve a inscrição que já existe. */
	public Subscribed subscribe(String email, Long productId, AlertType type) {
		String normalized = normalize(email);
		Optional<Subscription> existing = subscriptionRepository.findByEmailAndProductIdAndType(normalized,
				productId, type);
		if (existing.isPresent()) {
			return new Subscribed(existing.get(), false);
		}
		try {
			Subscription created = subscriptionRepository.save(
					new Subscription(normalized, productId, type, newToken(), clock.instant()));
			return new Subscribed(created, true);
		} catch (DataIntegrityViolationException ex) {
			// dois cliques ao mesmo tempo: o outro pedido gravou primeiro
			return subscriptionRepository.findByEmailAndProductIdAndType(normalized, productId, type)
					.map(subscription -> new Subscribed(subscription, false))
					.orElseThrow(() -> ex);
		}
	}

	public List<Subscription> list(String email, Long productId) {
		String normalized = normalize(email);
		boolean byEmail = StringUtils.hasText(normalized);
		if (byEmail && productId != null) {
			return subscriptionRepository.findByEmailAndProductIdOrderByConsentedAtDesc(normalized, productId);
		}
		if (byEmail) {
			return subscriptionRepository.findByEmailOrderByConsentedAtDesc(normalized);
		}
		if (productId != null) {
			return subscriptionRepository.findByProductIdOrderByConsentedAtDesc(productId);
		}
		return subscriptionRepository.findAllByOrderByConsentedAtDesc();
	}

	public void remove(Long id) {
		Subscription subscription = subscriptionRepository.findById(id)
				.orElseThrow(() -> new SubscriptionNotFoundException("Inscrição não encontrada."));
		subscriptionRepository.delete(subscription);
	}

	/** Pedido de exclusão dos dados de uma pessoa (LGPD). */
	public int removeAllByEmail(String email) {
		return subscriptionRepository.deleteAllByEmail(normalize(email));
	}

	/** O produto saiu do catálogo. */
	public int removeAllByProduct(Long productId) {
		return subscriptionRepository.deleteAllByProductId(productId);
	}

	public Optional<Subscription> findByToken(String token) {
		return subscriptionRepository.findByUnsubscribeToken(token);
	}

	/**
	 * Descadastro pelo link do e-mail. Token desconhecido não é erro: o link pode ser aberto
	 * mais de uma vez.
	 *
	 * @param allFromEmail cancela todos os avisos do mesmo e-mail, não só este
	 */
	public void unsubscribe(String token, boolean allFromEmail) {
		subscriptionRepository.findByUnsubscribeToken(token).ifPresent(subscription -> {
			if (allFromEmail) {
				subscriptionRepository.deleteAllByEmail(subscription.getEmail());
			} else {
				subscriptionRepository.delete(subscription);
			}
		});
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public record Subscribed(Subscription subscription, boolean created) {
	}
}
