package br.com.asobi.catalog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.catalog.dto.ProductForm;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.service.AdminProductService;
import br.com.asobi.catalog.service.CategoryService;
import br.com.asobi.common.exception.BusinessException;

@Controller
@RequestMapping("/admin/produtos")
public class AdminProductController {

	private final AdminProductService productService;
	private final CategoryService categoryService;

	public AdminProductController(AdminProductService productService, CategoryService categoryService) {
		this.productService = productService;
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("products", productService.listProducts());
		return "admin/products";
	}

	@GetMapping("/novo")
	public String newForm(Model model) {
		return renderForm(model, new ProductForm(), null);
	}

	@PostMapping
	public String create(@Validated @ModelAttribute("form") ProductForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		checkPrices(form, result);
		if (result.hasErrors()) {
			return renderForm(model, form, null);
		}
		try {
			Product product = productService.create(form);
			redirect.addFlashAttribute("success", "Produto \"" + product.getName() + "\" cadastrado.");
			return "redirect:/admin/produtos";
		} catch (BusinessException ex) {
			result.reject("business", ex.getMessage());
			return renderForm(model, form, null);
		}
	}

	@GetMapping("/{id}/editar")
	public String editForm(@PathVariable Long id, Model model) {
		return renderForm(model, productService.toForm(id), productService.getProduct(id));
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Validated @ModelAttribute("form") ProductForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		checkPrices(form, result);
		if (result.hasErrors()) {
			return renderForm(model, form, productService.getProduct(id));
		}
		try {
			Product product = productService.update(id, form);
			redirect.addFlashAttribute("success", "Produto \"" + product.getName() + "\" atualizado.");
			return "redirect:/admin/produtos";
		} catch (BusinessException ex) {
			result.reject("business", ex.getMessage());
			return renderForm(model, form, productService.getProduct(id));
		}
	}

	@PostMapping("/{id}/estoque")
	public String updateStock(@PathVariable Long id, @RequestParam int stock, RedirectAttributes redirect) {
		try {
			productService.updateStock(id, stock);
			redirect.addFlashAttribute("success", "Estoque atualizado.");
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/produtos";
	}

	@PostMapping("/{id}/encerrar-promocao")
	public String endSale(@PathVariable Long id, RedirectAttributes redirect) {
		productService.endSale(id);
		redirect.addFlashAttribute("success", "Promoção encerrada: o produto voltou ao preço normal.");
		return "redirect:/admin/produtos";
	}

	@PostMapping("/{id}/excluir")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		productService.delete(id);
		redirect.addFlashAttribute("success", "Produto excluído.");
		return "redirect:/admin/produtos";
	}

	private String renderForm(Model model, ProductForm form, Product product) {
		model.addAttribute("form", form);
		model.addAttribute("product", product);
		model.addAttribute("categories", categoryService.listForSelect());
		return "admin/product-form";
	}

	private static void checkPrices(ProductForm form, BindingResult result) {
		if (!form.isOnSale() || result.hasFieldErrors("regularPrice") || result.hasFieldErrors("salePrice")) {
			return;
		}
		if (form.getSalePrice() == null) {
			result.rejectValue("salePrice", "required", "Informe o preço promocional.");
		} else if (form.getSalePrice().compareTo(form.getRegularPrice()) >= 0) {
			result.rejectValue("salePrice", "invalid", "O preço promocional precisa ser menor que o preço normal.");
		}
	}
}
