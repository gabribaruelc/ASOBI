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
import br.com.asobi.content.dto.SiteInfoForm;
import br.com.asobi.content.service.SiteInfoService;

@Controller
@RequestMapping("/admin/contato")
public class AdminSiteInfoController {

	private final SiteInfoService siteInfoService;

	public AdminSiteInfoController(SiteInfoService siteInfoService) {
		this.siteInfoService = siteInfoService;
	}

	@GetMapping
	public String edit(Model model) {
		model.addAttribute("form", siteInfoService.toForm());
		return "admin/site-info";
	}

	@PostMapping
	public String update(@Validated @ModelAttribute("form") SiteInfoForm form, BindingResult result,
			Authentication authentication, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return "admin/site-info";
		}
		siteInfoService.update(form, CurrentAdmin.emailOf(authentication));
		redirect.addFlashAttribute("success", "Contato atualizado — aparece na loja em até 1 minuto.");
		return "redirect:/admin/contato";
	}
}
