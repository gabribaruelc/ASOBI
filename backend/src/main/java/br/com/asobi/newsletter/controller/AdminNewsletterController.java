package br.com.asobi.newsletter.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.newsletter.service.NewsletterCampaignService;
import br.com.asobi.newsletter.service.NewsletterService;

@Controller
@RequestMapping("/admin/newsletter")
public class AdminNewsletterController {

	private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

	private final NewsletterService newsletterService;
	private final NewsletterCampaignService campaignService;

	public AdminNewsletterController(NewsletterService newsletterService, NewsletterCampaignService campaignService) {
		this.newsletterService = newsletterService;
		this.campaignService = campaignService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("subscribers", newsletterService.list());
		model.addAttribute("campaigns", campaignService.list());
		return "admin/newsletter";
	}

	@GetMapping("/exportar")
	public ResponseEntity<String> export() {
		return ResponseEntity.ok()
				.contentType(CSV)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("newsletter-asobi.csv").build().toString())
				.body(newsletterService.exportCsv());
	}

	@PostMapping("/{id}/excluir")
	public String delete(@PathVariable Long id, RedirectAttributes redirect) {
		newsletterService.delete(id);
		redirect.addFlashAttribute("success", "E-mail removido da newsletter.");
		return "redirect:/admin/newsletter";
	}
}
