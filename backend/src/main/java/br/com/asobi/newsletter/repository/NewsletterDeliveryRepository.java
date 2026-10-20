package br.com.asobi.newsletter.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.newsletter.model.NewsletterDelivery;

public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, Long> {

	long countByCampaignId(Long campaignId);

	long countBySentAtAfter(Instant since);
}
