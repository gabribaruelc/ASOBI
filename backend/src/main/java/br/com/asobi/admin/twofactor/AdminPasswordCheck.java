package br.com.asobi.admin.twofactor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Segundo passo do login para a conta principal (admin@asobi.com.br): uma senha, conferida contra
 * o hash BCrypt da configuração. Depois de 5 erros em 15 minutos, a senha fica bloqueada por 15
 * minutos, mesmo se estiver certa.
 */
@Component
public class AdminPasswordCheck {

	static final int MAX_FAILURES = 5;
	static final Duration WINDOW = Duration.ofMinutes(15);

	public enum Result {
		OK, WRONG, LOCKED
	}

	private final String email;
	private final String hash;
	private final Clock clock;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final Deque<Instant> failures = new ArrayDeque<>();

	@Autowired
	public AdminPasswordCheck(AdminPasswordProperties properties) {
		this(properties, Clock.systemUTC());
	}

	AdminPasswordCheck(AdminPasswordProperties properties, Clock clock) {
		this.email = StringUtils.hasText(properties.email()) ? normalize(properties.email()) : null;
		this.hash = StringUtils.hasText(properties.hash()) ? properties.hash().trim() : null;
		this.clock = clock;
		if (hash != null && !hash.matches("\\$2[aby]?\\$\\d\\d\\$[./A-Za-z0-9]{53}")) {
			throw new IllegalStateException("ADMIN_PASSWORD_HASH não é um hash BCrypt válido.");
		}
	}

	/** Só vale com o hash configurado; sem ele, a conta segue o fluxo do código por e-mail. */
	public boolean appliesTo(String accountEmail) {
		return hash != null && email != null && email.equals(normalize(accountEmail));
	}

	public synchronized Result check(String password) {
		Instant now = clock.instant();
		while (!failures.isEmpty() && failures.peekFirst().isBefore(now.minus(WINDOW))) {
			failures.removeFirst();
		}
		if (failures.size() >= MAX_FAILURES) {
			return Result.LOCKED;
		}
		if (password != null && encoder.matches(password, hash)) {
			failures.clear();
			return Result.OK;
		}
		failures.addLast(now);
		return failures.size() >= MAX_FAILURES ? Result.LOCKED : Result.WRONG;
	}

	private static String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}
}
