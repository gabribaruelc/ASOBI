package br.com.asobi.admin.controller;

import org.springframework.security.core.Authentication;
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

import br.com.asobi.admin.security.CurrentAdmin;
import br.com.asobi.admin.service.AdminUserService;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Gestão de quem tem acesso ao painel (/admin/admins). */
@Controller
@RequestMapping("/admin/admins")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public String list(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new AdminUserForm(""));
		}
		model.addAttribute("admins", adminUserService.listAdmins());
		return "admin/admin-users";
	}

	@PostMapping
	public String add(@Validated @ModelAttribute("form") AdminUserForm form, BindingResult result,
			Authentication authentication, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			model.addAttribute("admins", adminUserService.listAdmins());
			return "admin/admin-users";
		}
		try {
			adminUserService.addAdmin(form.email(), CurrentAdmin.emailOf(authentication));
			redirect.addFlashAttribute("success", "Admin adicionado: " + AdminUserService.normalize(form.email()));
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/admins";
	}

	@PostMapping("/{id}/delete")
	public String remove(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
		try {
			adminUserService.removeAdmin(id, CurrentAdmin.emailOf(authentication));
			redirect.addFlashAttribute("success", "Acesso removido.");
		} catch (BusinessException | NotFoundException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/admins";
	}

	public record AdminUserForm(
			@NotBlank(message = "Informe o e-mail.")
			@Email(message = "E-mail inválido.")
			String email) {

		/** Tira espaços antes de validar (e-mail colado costuma vir com espaço). */
		public AdminUserForm {
			email = email == null ? null : email.trim();
		}
	}
}
