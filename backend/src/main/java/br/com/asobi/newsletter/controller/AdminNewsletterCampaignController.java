package br.com.asobi.newsletter.controller;

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
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.newsletter.dto.NewsletterCampaignForm;
import br.com.asobi.newsletter.model.NewsletterCampaign;
import br.com.asobi.newsletter.service.NewsletterCampaignService;
import br.com.asobi.newsletter.service.NewsletterCampaignService.CampaignStatus;
import br.com.asobi.newsletter.service.NewsletterCampaignService.SendResult;
import br.com.asobi.newsletter.service.NewsletterService;

@Controller
@RequestMapping("/admin/newsletter/campanhas")
public class AdminNewsletterCampaignController {

	private final NewsletterCampaignService campaignService;
	private final NewsletterService newsletterService;

	public AdminNewsletterCampaignController(NewsletterCampaignService campaignService,
			NewsletterService newsletterService) {
		this.campaignService = campaignService;
		this.newsletterService = newsletterService;
	}

	@GetMapping("/nova")
	public String newCampaign(Model model) {
		model.addAttribute("form", new NewsletterCampaignForm());
		addCommon(model);
		return "admin/newsletter-campaign";
	}

	@PostMapping
	public String create(@Validated @ModelAttribute("form") NewsletterCampaignForm form, BindingResult result,
			Authentication authentication, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			addCommon(model);
			return "admin/newsletter-campaign";
		}
		NewsletterCampaign campaign = campaignService.create(form, CurrentAdmin.emailOf(authentication));
		redirect.addFlashAttribute("success", "Rascunho salvo. Confira a prévia e mande um teste antes de enviar.");
		return "redirect:/admin/newsletter/campanhas/" + campaign.getId();
	}

	@GetMapping("/{id}")
	public String show(@PathVariable Long id, Model model) {
		CampaignStatus status = campaignService.get(id);
		model.addAttribute("form", campaignService.toForm(status.campaign()));
		addDetail(model, status);
		return "admin/newsletter-campaign";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id, @Validated @ModelAttribute("form") NewsletterCampaignForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			addDetail(model, campaignService.get(id));
			return "admin/newsletter-campaign";
		}
		try {
			campaignService.update(id, form);
			redirect.addFlashAttribute("success", "Rascunho salvo.");
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/newsletter/campanhas/" + id;
	}

	@PostMapping("/{id}/teste")
	public String sendTest(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
		String email = CurrentAdmin.emailOf(authentication);
		try {
			campaignService.sendTest(id, email);
			redirect.addFlashAttribute("success", "Teste enviado para " + email + ".");
		} catch (RuntimeException ex) {
			redirect.addFlashAttribute("error", "Não foi possível enviar o teste. Tente de novo em instantes.");
		}
		return "redirect:/admin/newsletter/campanhas/" + id;
	}

	@PostMapping("/{id}/envio")
	public String send(@PathVariable Long id, RedirectAttributes redirect) {
		SendResult result = campaignService.send(id);
		String sent = result.sent() == 1 ? "Enviada para 1 inscrito." : "Enviada para " + result.sent() + " inscritos.";
		switch (result.outcome()) {
			case DONE -> redirect.addFlashAttribute("success", sent + " Pronto, todo mundo recebeu! 🎉");
			case DAILY_LIMIT -> redirect.addFlashAttribute("success", sent + " Faltam " + result.pending()
					+ ": o limite de e-mails do dia acabou. Volte amanhã e clique em \"Continuar envio\".");
			case FAILED -> redirect.addFlashAttribute("error", (result.sent() > 0 ? sent + " " : "")
					+ "O serviço de e-mail recusou o envio. Ninguém recebeu duas vezes; tente continuar mais tarde.");
			case ALREADY_RUNNING -> redirect.addFlashAttribute("error",
					"Esta newsletter já está sendo enviada. Atualize a página em alguns instantes.");
		}
		return "redirect:/admin/newsletter/campanhas/" + id;
	}

	@PostMapping("/{id}/excluir")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			campaignService.delete(id);
			redirect.addFlashAttribute("success", "Rascunho excluído.");
			return "redirect:/admin/newsletter";
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
			return "redirect:/admin/newsletter/campanhas/" + id;
		}
	}

	private void addDetail(Model model, CampaignStatus status) {
		model.addAttribute("status", status);
		model.addAttribute("previewHtml", campaignService.preview(status.campaign().getId()));
		addCommon(model);
	}

	private void addCommon(Model model) {
		model.addAttribute("subscriberCount", newsletterService.count());
		model.addAttribute("dailyLimit", campaignService.dailyLimit());
		model.addAttribute("remainingQuota", campaignService.remainingQuota());
	}
}
