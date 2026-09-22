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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.catalog.dto.CategoryForm;
import br.com.asobi.catalog.service.CategoryService;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;

/** Faixas etárias (/admin/categorias). */
@Controller
@RequestMapping("/admin/categorias")
public class AdminCategoryController {

	private final CategoryService categoryService;

	public AdminCategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public String list(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new CategoryForm());
		}
		model.addAttribute("categories", categoryService.listCategories());
		return "admin/categories";
	}

	@PostMapping
	public String create(@Validated @ModelAttribute("form") CategoryForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("categories", categoryService.listCategories());
			return "admin/categories";
		}
		try {
			categoryService.create(form);
			redirect.addFlashAttribute("success", "Faixa etária \"" + form.getName().trim() + "\" criada.");
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/categorias";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Validated @ModelAttribute CategoryForm form, BindingResult result,
			RedirectAttributes redirect) {
		if (result.hasErrors()) {
			redirect.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
			return "redirect:/admin/categorias";
		}
		try {
			categoryService.update(id, form);
			redirect.addFlashAttribute("success", "Faixa etária atualizada.");
		} catch (NotFoundException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/categorias";
	}

	@PostMapping("/{id}/excluir")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			categoryService.delete(id);
			redirect.addFlashAttribute("success", "Faixa etária excluída.");
		} catch (BusinessException | NotFoundException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/categorias";
	}
}
