package br.com.asobi.notification.unsubscribe;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.asobi.notification.config.NotificationProperties;
import br.com.asobi.notification.subscription.SubscriptionService;

/**
 * Página aberta pelo link de descadastro do e-mail: a única parte do serviço que o navegador
 * acessa direto, sem X-Api-Key (quem autoriza é o token do link). O GET só mostra a
 * confirmação, porque antivírus e programas de e-mail abrem os links sozinhos; quem cancela
 * é o POST.
 */
@Controller
@RequestMapping("/descadastro/{token}")
public class UnsubscribeController {

	private final SubscriptionService subscriptionService;
	private final NotificationProperties properties;

	public UnsubscribeController(SubscriptionService subscriptionService, NotificationProperties properties) {
		this.subscriptionService = subscriptionService;
		this.properties = properties;
	}

	@GetMapping
	public String confirm(@PathVariable String token, Model model) {
		model.addAttribute("storeUrl", properties.storeUrl());
		model.addAttribute("state", subscriptionService.findByToken(token).isPresent() ? "confirm" : "done");
		return "unsubscribe";
	}

	@PostMapping
	public String unsubscribe(@PathVariable String token, @RequestParam(defaultValue = "false") boolean all,
			Model model) {
		subscriptionService.unsubscribe(token, all);
		model.addAttribute("storeUrl", properties.storeUrl());
		model.addAttribute("state", "done");
		return "unsubscribe";
	}
}
