package br.com.asobi.settings.controller;

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
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.settings.dto.StoreSettingsForm;
import br.com.asobi.settings.service.StoreSettingsService;
import br.com.asobi.shipping.ShippingQuoteProvider;

/** Configurações da loja (/admin/configuracoes): liga/desliga frete automático e parâmetros. */
@Controller
@RequestMapping("/admin/configuracoes")
public class AdminSettingsController {

	private final StoreSettingsService settingsService;
	private final ShippingQuoteProvider shippingQuoteProvider;

	public AdminSettingsController(StoreSettingsService settingsService, ShippingQuoteProvider shippingQuoteProvider) {
		this.settingsService = settingsService;
		this.shippingQuoteProvider = shippingQuoteProvider;
	}

	@ModelAttribute("shippingProviderConfigured")
	public boolean shippingProviderConfigured() {
		return shippingQuoteProvider.isConfigured();
	}

	@GetMapping
	public String edit(Model model) {
		model.addAttribute("form", settingsService.toForm());
		return "admin/settings";
	}

	@PostMapping
	public String update(@Validated @ModelAttribute("form") StoreSettingsForm form, BindingResult result,
			Authentication authentication, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return "admin/settings";
		}
		try {
			settingsService.update(form, CurrentAdmin.emailOf(authentication));
		} catch (BusinessException ex) {
			model.addAttribute("error", ex.getMessage());
			return "admin/settings";
		}
		redirect.addFlashAttribute("success", form.isShippingEnabled()
				? "Configurações salvas. O frete automático está LIGADO na loja."
				: "Configurações salvas. Frete automático desligado: na loja aparece \"a combinar\".");
		return "redirect:/admin/configuracoes";
	}
}
