package br.com.asobi.newsletter.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.common.time.DateFormats;
import br.com.asobi.newsletter.model.NewsletterSubscriber;
import br.com.asobi.newsletter.repository.NewsletterSubscriberRepository;

@Service
@Transactional
public class NewsletterService {

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
			subscriberRepository.save(new NewsletterSubscriber(normalized));
		}
	}

	@Transactional(readOnly = true)
	public List<NewsletterSubscriber> list() {
		return subscriberRepository.findAllByOrderByCreatedAtDesc();
	}

	public void delete(Long id) {
		subscriberRepository.delete(subscriberRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Inscrição não encontrada.")));
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

	/** Um e-mail começando com =, +, - ou @ seria lido como fórmula pelo Excel. */
	private static String csvSafe(String value) {
		return "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
	}
}
