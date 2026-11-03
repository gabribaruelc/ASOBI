package br.com.asobi.status.recipient;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Para quem vai o aviso: todos os admins cadastrados no painel. Se o admin-auth-service não
 * responder (ele também pode ter caído), usa a última lista guardada; sem nenhuma, o fallback.
 */
@Component
public class AlertRecipients {

	private static final Logger log = LoggerFactory.getLogger(AlertRecipients.class);

	private final AdminListClient client;
	private final KnownAdminRepository repository;
	private final List<String> fallback;

	public AlertRecipients(AdminListClient client, KnownAdminRepository repository,
			@Value("${status.fallback-emails:}") String fallbackEmails) {
		this.client = client;
		this.repository = repository;
		this.fallback = Arrays.stream(fallbackEmails.split(","))
				.map(AlertRecipients::normalize)
				.filter(StringUtils::hasText)
				.distinct()
				.toList();
	}

	/** Atualiza a cópia da lista; chamado a cada rodada, esteja tudo bem ou não. */
	@Transactional
	public void refresh() {
		if (!client.isConfigured()) {
			return;
		}
		try {
			List<String> admins = client.listAdminEmails().stream()
					.map(AlertRecipients::normalize)
					.filter(StringUtils::hasText)
					.distinct()
					.toList();
			if (!admins.isEmpty()) {
				List<String> stored = repository.findAll().stream().map(KnownAdmin::getEmail).toList();
				repository.deleteAllById(stored.stream().filter(email -> !admins.contains(email)).toList());
				repository.saveAll(admins.stream().filter(email -> !stored.contains(email)).map(KnownAdmin::new).toList());
			}
		} catch (RuntimeException ex) {
			log.warn("Lista de admins indisponível agora; os avisos vão para a última lista guardada: {}",
					ex.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public List<String> current() {
		List<String> known = repository.findAll().stream().map(KnownAdmin::getEmail).sorted().toList();
		return known.isEmpty() ? fallback : known;
	}

	private static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
