package br.com.asobi.content.dto;

import java.time.Instant;

import br.com.asobi.content.model.PolicyPage;

public record PolicyPageResponse(String slug, String title, String body, Instant updatedAt) {

	public static PolicyPageResponse from(PolicyPage page) {
		return new PolicyPageResponse(page.getSlug(), page.getTitle(), page.getBody(), page.getUpdatedAt());
	}
}
