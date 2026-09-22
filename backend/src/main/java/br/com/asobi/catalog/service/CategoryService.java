package br.com.asobi.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.dto.CategoryForm;
import br.com.asobi.catalog.model.Category;
import br.com.asobi.catalog.repository.CategoryRepository;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.common.text.Slugs;

/** Faixas etárias, gerenciadas pelo painel admin. */
@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryRow> listCategories() {
		return categoryRepository.findAllByOrderByPositionAscNameAsc().stream()
				.map(category -> new CategoryRow(category.getId(), category.getSlug(), category.getName(),
						category.getPosition(), productRepository.countByCategoryId(category.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Category> listForSelect() {
		return categoryRepository.findAllByOrderByPositionAscNameAsc();
	}

	/** O slug (usado na URL da loja, ex.: /jogos?idade=4-a-6-anos) é gerado do nome e não muda depois. */
	public Category create(CategoryForm form) {
		String slug = Slugs.slugify(form.getName());
		if (slug.isEmpty()) {
			throw new BusinessException("Nome inválido.");
		}
		if (categoryRepository.existsBySlug(slug)) {
			throw new BusinessException("Já existe uma faixa etária com esse nome.");
		}
		return categoryRepository.save(new Category(slug, form.getName().trim(), form.getPosition()));
	}

	public void update(Long id, CategoryForm form) {
		Category category = get(id);
		category.setName(form.getName().trim());
		category.setPosition(form.getPosition());
	}

	public void delete(Long id) {
		Category category = get(id);
		if (productRepository.existsByCategoryId(id)) {
			throw new BusinessException("Não dá para excluir: ainda há produtos nessa faixa etária. Mude-os de faixa antes.");
		}
		categoryRepository.delete(category);
	}

	private Category get(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Faixa etária não encontrada."));
	}

	public record CategoryRow(Long id, String slug, String name, int position, long productCount) {
	}
}
