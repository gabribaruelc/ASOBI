package br.com.asobi.catalog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.catalog.dto.CategoryResponse;
import br.com.asobi.catalog.dto.ProductResponse;
import br.com.asobi.catalog.repository.ProductSpecifications.ProductFilter;
import br.com.asobi.catalog.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {

	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/categories")
	public List<CategoryResponse> listCategories() {
		return catalogService.listCategories();
	}

	/**
	 * Ex.: /api/products?category=4-6, ?cooperative=true, ?isNew=true, ?onSale=true
	 */
	@GetMapping("/products")
	public List<ProductResponse> listProducts(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) Boolean cooperative,
			@RequestParam(required = false) Boolean isNew,
			@RequestParam(required = false) Boolean onSale) {
		return catalogService.listProducts(new ProductFilter(category, cooperative, isNew, onSale));
	}

	@GetMapping("/products/{slug}")
	public ProductResponse getProduct(@PathVariable String slug) {
		return catalogService.getProduct(slug);
	}
}
