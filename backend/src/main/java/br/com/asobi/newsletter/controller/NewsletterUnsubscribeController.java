package br.com.asobi.newsletter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import br.com.asobi.newsletter.service.NewsletterService;

/**
 * Página do link de descadastro da newsletter, aberta direto pelo navegador (quem autoriza é
 * o token do link). O GET só mostra a confirmação, porque antivírus e programas de e-mail
 * abrem os links sozinhos; quem cancela é o POST, que também atende o botão de descadastro
 * do próprio programa de e-mail (List-Unsubscribe-Post).
 */
@Controller
@RequestMapping("/newsletter/descadastro/{token}")
public class NewsletterUnsubscribeController {

	private final NewsletterService newsletterService;

	public NewsletterUnsubscribeController(NewsletterService newsletterService) {
		this.newsletterService = newsletterService;
	}

	@GetMapping
	public String confirm(@PathVariable String token, Model model) {
		boolean subscribed = newsletterService.findByUnsubscribeToken(token).isPresent();
		model.addAttribute("state", subscribed ? "confirm" : "done");
		return "newsletter-unsubscribe";
	}

	@PostMapping
	public String unsubscribe(@PathVariable String token, Model model) {
		newsletterService.unsubscribe(token);
		model.addAttribute("state", "done");
		return "newsletter-unsubscribe";
	}
}
