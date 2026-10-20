package br.com.asobi.newsletter.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.common.time.DateFormats;
import br.com.asobi.newsletter.model.NewsletterSubscriber;
import br.com.asobi.newsletter.repository.NewsletterSubscriberRepository;

@Service
@Transactional
public class NewsletterService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final NewsletterSubscriberRepository subscriberRepository;
	private final DateFormats dates;

	public NewsletterService(NewsletterSubscriberRepository subscriberRepository, DateFormats dates) {
		this.subscriberRepository = subscriberRepository;
		this.dates = dates;
	}

	/** Inscrever de novo o mesmo e-mail não é erro (e não revela quem já está na lista). */
	public void subscribe(String email) {
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		if (!subscriberRepository.existsByEmail(normalized)) {
			subscriberRepository.save(new NewsletterSubscriber(normalized, newUnsubscribeToken()));
		}
	}

	@Transactional(readOnly = true)
	public List<NewsletterSubscriber> list() {
		return subscriberRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public long count() {
		return subscriberRepository.count();
	}

	public void delete(Long id) {
		subscriberRepository.delete(subscriberRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Inscrição não encontrada.")));
	}

	@Transactional(readOnly = true)
	public Optional<NewsletterSubscriber> findByUnsubscribeToken(String token) {
		return subscriberRepository.findByUnsubscribeToken(token);
	}

	/**
	 * Descadastro pelo link do e-mail: apaga o e-mail da lista (LGPD). Token desconhecido não é
	 * erro, porque o link pode ser aberto mais de uma vez.
	 */
	public void unsubscribe(String token) {
		subscriberRepository.findByUnsubscribeToken(token).ifPresent(subscriberRepository::delete);
	}

	/** Lista para importar numa ferramenta de envio de e-mail. */
	@Transactional(readOnly = true)
	public String exportCsv() {
		StringBuilder csv = new StringBuilder("email,inscrito_em\n");
		for (NewsletterSubscriber subscriber : list()) {
			csv.append(csvSafe(subscriber.getEmail())).append(',')
					.append(dates.dateTime(subscriber.getCreatedAt())).append('\n');
		}
		return csv.toString();
	}

	static String newUnsubscribeToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Um e-mail começando com =, +, - ou @ seria lido como fórmula pelo Excel. */
	private static String csvSafe(String value) {
		return "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
	}
}
