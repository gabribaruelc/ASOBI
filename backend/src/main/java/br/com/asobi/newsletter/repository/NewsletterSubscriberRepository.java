package br.com.asobi.newsletter.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.asobi.newsletter.model.NewsletterSubscriber;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

	boolean existsByEmail(String email);

	List<NewsletterSubscriber> findAllByOrderByCreatedAtDesc();

	Optional<NewsletterSubscriber> findByUnsubscribeToken(String unsubscribeToken);

	/** Quem estava inscrito até {@code until} e ainda não recebeu a newsletter. */
	@Query("""
			SELECT s FROM NewsletterSubscriber s
			WHERE s.createdAt <= :until
			  AND NOT EXISTS (SELECT 1 FROM NewsletterDelivery d
			                  WHERE d.campaignId = :campaignId AND d.subscriberId = s.id)
			ORDER BY s.id""")
	List<NewsletterSubscriber> findPending(@Param("campaignId") Long campaignId, @Param("until") Instant until,
			Pageable page);

	@Query("""
			SELECT COUNT(s) FROM NewsletterSubscriber s
			WHERE s.createdAt <= :until
			  AND NOT EXISTS (SELECT 1 FROM NewsletterDelivery d
			                  WHERE d.campaignId = :campaignId AND d.subscriberId = s.id)""")
	long countPending(@Param("campaignId") Long campaignId, @Param("until") Instant until);
}
