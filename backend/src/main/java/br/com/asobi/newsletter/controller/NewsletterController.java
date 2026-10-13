package br.com.asobi.newsletter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.newsletter.dto.NewsletterSubscriptionRequest;
import br.com.asobi.newsletter.service.NewsletterService;

@RestController
public class NewsletterController {

	private final NewsletterService newsletterService;

	public NewsletterController(NewsletterService newsletterService) {
		this.newsletterService = newsletterService;
	}

	@PostMapping("/api/newsletter-subscriptions")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribe(@Validated @RequestBody NewsletterSubscriptionRequest request) {
		newsletterService.subscribe(request.email());
	}
}
