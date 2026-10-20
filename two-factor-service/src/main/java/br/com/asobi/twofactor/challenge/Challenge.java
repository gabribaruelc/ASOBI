package br.com.asobi.twofactor.challenge;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Um código de uso único enviado para um e-mail. Guarda só o HMAC do código. */
@Entity
@Table(name = "challenges")
public class Challenge {

	@Id
	private UUID id;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(nullable = false, length = 40)
	private String purpose;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	protected Challenge() {
	}

	public Challenge(UUID id, String email, String purpose, String codeHash, Instant createdAt, Instant expiresAt) {
		this.id = id;
		this.email = email;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	/** Ainda aceita tentativa: não foi usado, não venceu e não estourou o limite de erros. */
	public boolean isOpen(Instant now, int maxAttempts) {
		return consumedAt == null && now.isBefore(expiresAt) && attempts < maxAttempts;
	}

	public void registerFailedAttempt() {
		attempts++;
	}

	public void consume(Instant now) {
		consumedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPurpose() {
		return purpose;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public int getAttempts() {
		return attempts;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}
}
