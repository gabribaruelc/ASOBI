package br.com.asobi.newsletter.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma newsletter enviada (ou sendo enviada agora) para um inscrito. */
@Entity
@Table(name = "newsletter_deliveries")
public class NewsletterDelivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "campaign_id", nullable = false, updatable = false)
	private Long campaignId;

	@Column(name = "subscriber_id", nullable = false, updatable = false)
	private Long subscriberId;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private Instant sentAt;

	protected NewsletterDelivery() {
	}

	public NewsletterDelivery(Long campaignId, Long subscriberId, Instant sentAt) {
		this.campaignId = campaignId;
		this.subscriberId = subscriberId;
		this.sentAt = sentAt;
	}

	public Long getId() {
		return id;
	}

	public Long getCampaignId() {
		return campaignId;
	}

	public Long getSubscriberId() {
		return subscriberId;
	}

	public Instant getSentAt() {
		return sentAt;
	}
}
