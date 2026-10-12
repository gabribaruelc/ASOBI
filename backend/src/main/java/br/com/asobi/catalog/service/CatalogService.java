package br.com.asobi.catalog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.dto.CategoryResponse;
import br.com.asobi.catalog.dto.ProductResponse;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.CategoryRepository;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.catalog.repository.ProductSpecifications;
import br.com.asobi.catalog.repository.ProductSpecifications.ProductFilter;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.review.model.Review;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

/** Leitura pública do catálogo, usada pela loja (Next.js). */
@Service
@Transactional(readOnly = true)
public class CatalogService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final ReviewRepository reviewRepository;
	private final ProductImageService imageService;
	private final Clock clock;

	public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
			ReviewRepository reviewRepository, ProductImageService imageService, Clock clock) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
		this.imageService = imageService;
		this.clock = clock;
	}

	public List<CategoryResponse> listCategories() {
		return categoryRepository.findAllByOrderByPositionAscNameAsc().stream()
				.map(CategoryResponse::from)
				.toList();
	}

	public List<ProductResponse> listProducts(ProductFilter filter) {
		Instant now = clock.instant();
		List<Product> products = productRepository.findAll(ProductSpecifications.matching(filter, now),
				Sort.by("name"));
		return toResponses(products, now);
	}

	public ProductResponse getProduct(String slug) {
		Product product = productRepository.findBySlug(slug)
				.orElseThrow(() -> new NotFoundException("Produto não encontrado: " + slug));
		return toResponses(List.of(product), clock.instant()).get(0);
	}

	/** Busca as avaliações aprovadas de todos os produtos numa única consulta. */
	private List<ProductResponse> toResponses(List<Product> products, Instant now) {
		if (products.isEmpty()) {
			return List.of();
		}
		List<Long> ids = products.stream().map(Product::getId).toList();
		Map<Long, List<Review>> reviewsByProduct = reviewRepository
				.findByProductIdInAndStatusOrderByCreatedAtAsc(ids, ReviewStatus.APPROVED).stream()
				.collect(Collectors.groupingBy(review -> review.getProduct().getId()));
		return products.stream()
				.map(product -> ProductResponse.from(product, imageService.urls(product),
						reviewsByProduct.getOrDefault(product.getId(), List.of()), now))
				.toList();
	}
}
