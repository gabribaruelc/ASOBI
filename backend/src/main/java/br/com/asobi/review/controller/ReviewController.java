package br.com.asobi.review.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.review.dto.ReviewRequest;
import br.com.asobi.review.service.ReviewService;

@RestController
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	/** Recebe a avaliação como pendente; ela só aparece na loja depois de aprovada no painel. */
	@PostMapping("/api/products/{slug}/reviews")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, String> submit(@PathVariable String slug, @Validated @RequestBody ReviewRequest request) {
		reviewService.submit(slug, request);
		return Map.of("status", "pending");
	}
}
