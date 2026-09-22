package br.com.asobi.catalog.dto;

import br.com.asobi.catalog.model.Category;

public record CategoryResponse(String slug, String name) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getSlug(), category.getName());
	}
}
