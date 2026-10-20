package br.com.asobi.twofactor.challenge;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.twofactor.config.TwoFactorProperties;
import br.com.asobi.twofactor.email.CodeEmailComposer;
import br.com.asobi.twofactor.email.EmailSender;

@Service
public class ChallengeService {

	private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

	// Não há agendador (o Cloud Run escala a zero): a limpeza pega carona em cada pedido de código.
	private static final Duration RETENTION = Duration.ofDays(1);

	private final ChallengeRepository challengeRepository;
	private final CodeHasher codeHasher;
	private final CodeEmailComposer composer;
	private final EmailSender emailSender;
	private final TwoFactorProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public ChallengeService(ChallengeRepository challengeRepository, CodeHasher codeHasher,
			CodeEmailComposer composer, EmailSender emailSender, TwoFactorProperties properties, Clock clock) {
		this.challengeRepository = challengeRepository;
		this.codeHasher = codeHasher;
		this.composer = composer;
		this.emailSender = emailSender;
		this.properties = properties;
		this.clock = clock;
	}

	public static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Gera o código, grava o hash e envia o e-mail. Sem transação de propósito: a chamada ao
	 * Resend não pode segurar uma conexão do banco. Se o envio falhar, o desafio é apagado e
	 * o código anterior (se houver) continua valendo.
	 * <p>
	 * Pedidos repetidos dentro de {@code resendCooldown} recebem o desafio que já existe, sem
	 * novo e-mail. Dois pedidos simultâneos gravam cada um o seu e depois conferem: fica o mais
	 * antigo e o outro se apaga antes de enviar. Assim sai um e-mail só, e o código dele vale.
	 */
	public Challenge create(String email, String purpose) {
		String normalized = normalize(email);
		Instant now = clock.instant();
		challengeRepository.deleteExpiredBefore(now.minus(RETENTION));

		Instant cooldownStart = now.minus(properties.resendCooldown());
		List<Challenge> pending = challengeRepository.findOpenCreatedAfter(normalized, purpose, cooldownStart, now);
		if (!pending.isEmpty()) {
			return pending.get(0);
		}

		long recent = challengeRepository.countByEmailAndCreatedAtAfter(normalized,
				now.minus(properties.challengeWindow()));
		if (recent >= properties.maxChallenges()) {
			throw new TooManyChallengesException("Muitos códigos pedidos para este e-mail. Tente de novo mais tarde.");
		}

		UUID id = UUID.randomUUID();
		String code = String.format("%06d", random.nextInt(1_000_000));
		Challenge challenge = challengeRepository.save(new Challenge(id, normalized, purpose,
				codeHasher.hash(id, code), now, now.plus(properties.codeTtl())));

		Challenge first = challengeRepository.findOpenCreatedAfter(normalized, purpose, cooldownStart, now).get(0);
		if (!first.getId().equals(id)) {
			challengeRepository.delete(challenge);
			return first;
		}
		try {
			emailSender.send(composer.compose(normalized, code, properties.codeTtl()));
		} catch (RuntimeException ex) {
			log.error("Falha ao enviar o código do desafio {}", id, ex);
			challengeRepository.delete(challenge);
			throw new EmailDeliveryException("Não foi possível enviar o código agora. Tente novamente em instantes.");
		}
		challengeRepository.expireOlder(normalized, purpose, id, challenge.getCreatedAt(), now);
		return challenge;
	}

	/** Não lança exceção para código errado: a tentativa gasta precisa ser gravada. */
	@Transactional
	public Verification verify(UUID id, String code) {
		Challenge challenge = challengeRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ChallengeNotFoundException("Código não encontrado. Peça um novo."));
		Instant now = clock.instant();
		if (!challenge.isOpen(now, properties.maxAttempts())) {
			return new Verification(Verification.Status.EXPIRED, challenge, 0);
		}
		if (!codeHasher.matches(id, code, challenge.getCodeHash())) {
			challenge.registerFailedAttempt();
			return new Verification(Verification.Status.WRONG_CODE, challenge,
					properties.maxAttempts() - challenge.getAttempts());
		}
		challenge.consume(now);
		return new Verification(Verification.Status.VERIFIED, challenge, 0);
	}

	public record Verification(Status status, Challenge challenge, int attemptsLeft) {

		public enum Status {
			VERIFIED, WRONG_CODE, EXPIRED
		}
	}
}
