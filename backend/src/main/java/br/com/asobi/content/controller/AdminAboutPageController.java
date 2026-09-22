package br.com.asobi.content.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.admin.security.CurrentAdmin;
import br.com.asobi.content.dto.AboutPageForm;
import br.com.asobi.content.service.AboutPageService;

/** Editor da página Sobre (/admin/sobre). */
@Controller
@RequestMapping("/admin/sobre")
public class AdminAboutPageController {

	private final AboutPageService aboutPageService;

	public AdminAboutPageController(AboutPageService aboutPageService) {
		this.aboutPageService = aboutPageService;
	}

	@GetMapping
	public String edit(Model model) {
		model.addAttribute("form", aboutPageService.toForm());
		return "admin/about-page";
	}

	@PostMapping
	public String update(@Validated @ModelAttribute("form") AboutPageForm form, BindingResult result,
			Authentication authentication, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return "admin/about-page";
		}
		aboutPageService.update(form, CurrentAdmin.emailOf(authentication));
		redirect.addFlashAttribute("success", "Página Sobre atualizada — já aparece na loja.");
		return "redirect:/admin/sobre";
	}
}
