package br.com.asobi.newsletter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.newsletter.model.NewsletterSubscriber;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

	boolean existsByEmail(String email);

	List<NewsletterSubscriber> findAllByOrderByCreatedAtDesc();
}
