package br.com.asobi.status.check;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Como estava cada serviço na última conferência. */
@Entity
@Table(name = "target_states")
public class TargetState {

	@Id
	private String name;

	@Column(nullable = false)
	private boolean up = true;

	@Column(name = "consecutive_failures", nullable = false)
	private int consecutiveFailures;

	@Column(name = "last_checked_at")
	private Instant lastCheckedAt;

	/** Primeira falha da sequência atual (continua preenchido até o aviso de volta sair). */
	@Column(name = "down_since")
	private Instant downSince;

	@Column(name = "last_error", length = 200)
	private String lastError;

	/** Último aviso de "fora do ar" enviado; null = nenhum aviso pendente de "voltou". */
	@Column(name = "alerted_at")
	private Instant alertedAt;

	protected TargetState() {
	}

	public TargetState(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isUp() {
		return up;
	}

	public int getConsecutiveFailures() {
		return consecutiveFailures;
	}

	public Instant getLastCheckedAt() {
		return lastCheckedAt;
	}

	public Instant getDownSince() {
		return downSince;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getAlertedAt() {
		return alertedAt;
	}

	void recordUp(Instant now) {
		up = true;
		consecutiveFailures = 0;
		lastError = null;
		lastCheckedAt = now;
		if (alertedAt == null) {
			downSince = null;
		}
	}

	void recordDown(Instant now, String error) {
		if (up && alertedAt == null) {
			downSince = now;
		}
		up = false;
		consecutiveFailures++;
		lastError = error;
		lastCheckedAt = now;
	}

	void markAlerted(Instant now) {
		alertedAt = now;
	}

	void markRecoveryNotified() {
		alertedAt = null;
		downSince = null;
	}
}
