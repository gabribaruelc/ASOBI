package br.com.asobi.newsletter.model;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/** Uma newsletter escrita no painel: rascunho até o primeiro envio. */
@Entity
@Table(name = "newsletter_campaigns")
public class NewsletterCampaign {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(nullable = false, length = 20000)
	private String body;

	@Column(name = "button_label", length = 60)
	private String buttonLabel;

	@Column(name = "button_url", length = 500)
	private String buttonUrl;

	/** Jogos em destaque (cartões com foto e preço). Carregados junto: o envio roda fora de transação. */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "newsletter_campaign_products", joinColumns = @JoinColumn(name = "campaign_id"))
	@Column(name = "product_id")
	private Set<Long> productIds = new LinkedHashSet<>();

	@Column(name = "created_by", nullable = false, updatable = false, length = 254)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "started_at")
	private Instant startedAt;

	protected NewsletterCampaign() {
	}

	public NewsletterCampaign(String createdBy) {
		this.createdBy = createdBy;
	}

	public void update(String subject, String body, String buttonLabel, String buttonUrl,
			Collection<Long> productIds) {
		this.subject = subject;
		this.body = body;
		this.buttonLabel = buttonLabel;
		this.buttonUrl = buttonUrl;
		this.productIds.clear();
		this.productIds.addAll(productIds);
	}

	public void markStarted(Instant now) {
		if (startedAt == null) {
			startedAt = now;
		}
	}

	public boolean isStarted() {
		return startedAt != null;
	}

	public Long getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public String getButtonLabel() {
		return buttonLabel;
	}

	public String getButtonUrl() {
		return buttonUrl;
	}

	public Set<Long> getProductIds() {
		return productIds;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}
}
