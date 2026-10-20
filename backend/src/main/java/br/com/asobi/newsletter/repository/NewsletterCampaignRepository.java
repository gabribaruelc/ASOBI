package br.com.asobi.newsletter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.newsletter.model.NewsletterCampaign;

public interface NewsletterCampaignRepository extends JpaRepository<NewsletterCampaign, Long> {

	List<NewsletterCampaign> findAllByOrderByCreatedAtDesc();
}
