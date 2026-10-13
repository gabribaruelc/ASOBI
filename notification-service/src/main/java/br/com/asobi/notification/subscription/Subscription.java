package br.com.asobi.notification.subscription;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pedido de aviso de um e-mail sobre um produto, feito com consentimento. */
@Entity
@Table(name = "subscriptions")
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "alert_type", nullable = false, length = 20)
	private AlertType type;

	@Column(name = "unsubscribe_token", nullable = false, unique = true, length = 64)
	private String unsubscribeToken;

	@Column(name = "consented_at", nullable = false, updatable = false)
	private Instant consentedAt;

	@Column(name = "last_notified_at")
	private Instant lastNotifiedAt;

	protected Subscription() {
	}

	public Subscription(String email, Long productId, AlertType type, String unsubscribeToken, Instant consentedAt) {
		this.email = email;
		this.productId = productId;
		this.type = type;
		this.unsubscribeToken = unsubscribeToken;
		this.consentedAt = consentedAt;
	}

	/** Já pode receber outro aviso: nunca recebeu ou o intervalo mínimo passou. */
	public boolean isDue(Instant now, Duration cooldown) {
		return lastNotifiedAt == null || !lastNotifiedAt.plus(cooldown).isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public Long getProductId() {
		return productId;
	}

	public AlertType getType() {
		return type;
	}

	public String getUnsubscribeToken() {
		return unsubscribeToken;
	}

	public Instant getConsentedAt() {
		return consentedAt;
	}

	public Instant getLastNotifiedAt() {
		return lastNotifiedAt;
	}
}
