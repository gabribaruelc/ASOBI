package br.com.asobi.newsletter.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "newsletter_subscribers")
public class NewsletterSubscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	/** Vazio só nas inscrições anteriores ao envio pelo painel (ver V15). */
	@Column(name = "unsubscribe_token", unique = true, length = 64)
	private String unsubscribeToken;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected NewsletterSubscriber() {
	}

	public NewsletterSubscriber(String email, String unsubscribeToken) {
		this.email = email;
		this.unsubscribeToken = unsubscribeToken;
	}

	public void assignUnsubscribeToken(String unsubscribeToken) {
		this.unsubscribeToken = unsubscribeToken;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getUnsubscribeToken() {
		return unsubscribeToken;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
