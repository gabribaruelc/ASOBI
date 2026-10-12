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
import br.com.asobi.content.dto.PoliciesForm;
import br.com.asobi.content.service.PolicyPageService;

@Controller
@RequestMapping("/admin/politicas")
public class AdminPolicyController {

	private final PolicyPageService policyPageService;

	public AdminPolicyController(PolicyPageService policyPageService) {
		this.policyPageService = policyPageService;
	}

	@GetMapping
	public String edit(Model model) {
		model.addAttribute("form", policyPageService.toForm());
		return "admin/policies";
	}

	@PostMapping
	public String update(@Validated @ModelAttribute("form") PoliciesForm form, BindingResult result,
			Authentication authentication, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return "admin/policies";
		}
		policyPageService.update(form, CurrentAdmin.emailOf(authentication));
		redirect.addFlashAttribute("success", "Políticas atualizadas — aparecem na loja em até 1 minuto.");
		return "redirect:/admin/politicas";
	}
}
