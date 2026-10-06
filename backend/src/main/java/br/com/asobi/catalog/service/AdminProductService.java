package br.com.asobi.catalog.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import br.com.asobi.catalog.dto.AdminProductRow;
import br.com.asobi.catalog.dto.ProductForm;
import br.com.asobi.catalog.model.Category;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.CategoryRepository;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.common.text.Slugs;
import br.com.asobi.common.time.StoreTime;

/** Cadastro de produtos pelo painel admin. */
@Service
@Transactional
public class AdminProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductImageService imageService;
	private final Clock clock;

	public AdminProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
			ProductImageService imageService, Clock clock) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.imageService = imageService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AdminProductRow> listProducts() {
		Instant now = clock.instant();
		return productRepository.findAllByOrderByNameAsc().stream()
				.map(product -> AdminProductRow.from(product, now,
						imageService.urls(product).stream().findFirst().orElse(null)))
				.toList();
	}

	@Transactional(readOnly = true)
	public Product getProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
	}

	@Transactional(readOnly = true)
	public ProductForm toForm(Long id) {
		Product product = getProduct(id);
		ProductForm form = new ProductForm();
		form.setName(product.getName());
		form.setIcon(product.getIcon());
		form.setDescription(product.getDescription());
		form.setSkill(product.getSkill());
		form.setCategoryId(product.getCategory().getId());
		form.setAgeLabel(product.getAgeLabel());
		form.setPlayers(product.getPlayers());
		form.setOnSale(product.isOnSale());
		form.setRegularPrice(product.isOnSale() ? product.getOriginalPrice() : product.getPrice());
		form.setSalePrice(product.isOnSale() ? product.getPrice() : null);
		form.setStock(product.getStock());
		form.setCooperative(product.isCooperative());
		form.setNewUntil(StoreTime.lastDay(product.getNewUntil()));
		return form;
	}

	public Product create(ProductForm form, List<MultipartFile> photos) {
		imageService.validateForNewProduct(photos);
		Product product = new Product();
		product.setSlug(uniqueSlug(form.getName()));
		apply(product, form);
		product = productRepository.save(product);
		imageService.add(product, photos);
		return product;
	}

	/** O slug não muda na edição: é o link público do produto. As fotos enviadas são acrescentadas. */
	public Product update(Long id, ProductForm form, List<MultipartFile> photos) {
		Product product = getProduct(id);
		apply(product, form);
		imageService.add(product, photos);
		return product;
	}

	public void updateStock(Long id, int stock) {
		if (stock < 0) {
			throw new BusinessException("O estoque não pode ser negativo.");
		}
		getProduct(id).setStock(stock);
	}

	/** Tira da promoção: volta a cobrar o preço normal. */
	public void endSale(Long id) {
		Product product = getProduct(id);
		if (product.isOnSale()) {
			product.setPrice(product.getOriginalPrice());
			product.setOriginalPrice(null);
		}
	}

	public void delete(Long id) {
		Product product = getProduct(id);
		imageService.deleteFiles(product);
		productRepository.delete(product);
	}

	private void apply(Product product, ProductForm form) {
		if (form.isOnSale()) {
			if (form.getSalePrice() == null || form.getSalePrice().compareTo(form.getRegularPrice()) >= 0) {
				throw new BusinessException("O preço promocional precisa ser menor que o preço normal.");
			}
			product.setPrice(form.getSalePrice());
			product.setOriginalPrice(form.getRegularPrice());
		} else {
			product.setPrice(form.getRegularPrice());
			product.setOriginalPrice(null);
		}
		Category category = categoryRepository.findById(form.getCategoryId())
				.orElseThrow(() -> new BusinessException("Faixa etária inválida."));
		product.setName(form.getName().trim());
		product.setIcon(StringUtils.hasText(form.getIcon()) ? form.getIcon().trim() : null);
		product.setDescription(form.getDescription().trim());
		product.setSkill(form.getSkill().trim());
		product.setCategory(category);
		product.setAgeLabel(form.getAgeLabel().trim());
		product.setPlayers(form.getPlayers().trim());
		product.setStock(form.getStock());
		product.setCooperative(form.isCooperative());
		product.setNewUntil(StoreTime.endOfDay(form.getNewUntil()));
	}

	private String uniqueSlug(String name) {
		String base = Slugs.slugify(name);
		if (base.isEmpty()) {
			base = "produto";
		}
		String candidate = base;
		for (int suffix = 2; productRepository.existsBySlug(candidate); suffix++) {
			candidate = base + "-" + suffix;
		}
		return candidate;
	}
}
