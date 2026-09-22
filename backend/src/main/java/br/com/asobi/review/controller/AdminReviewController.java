package br.com.asobi.review.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.service.ReviewService;

/** Fila de moderação (/admin/avaliacoes). */
@Controller
@RequestMapping("/admin/avaliacoes")
public class AdminReviewController {

	/** Nome da aba na URL (?status=aprovadas) → status no banco. */
	private static final Map<String, ReviewStatus> TABS = Map.of(
			"pendentes", ReviewStatus.PENDING,
			"aprovadas", ReviewStatus.APPROVED,
			"rejeitadas", ReviewStatus.REJECTED);

	private final ReviewService reviewService;

	public AdminReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "pendentes") String status, Model model) {
		ReviewStatus reviewStatus = TABS.getOrDefault(status, ReviewStatus.PENDING);
		model.addAttribute("tab", TABS.containsKey(status) ? status : "pendentes");
		model.addAttribute("reviews", reviewService.listByStatus(reviewStatus));
		model.addAttribute("pendingCount", reviewService.countByStatus(ReviewStatus.PENDING));
		return "admin/reviews";
	}

	@PostMapping("/{id}/aprovar")
	public String approve(@PathVariable Long id, @RequestParam(defaultValue = "pendentes") String tab,
			RedirectAttributes redirect) {
		reviewService.changeStatus(id, ReviewStatus.APPROVED);
		redirect.addFlashAttribute("success", "Avaliação aprovada — já aparece na loja.");
		return redirectTo(tab);
	}

	@PostMapping("/{id}/rejeitar")
	public String reject(@PathVariable Long id, @RequestParam(defaultValue = "pendentes") String tab,
			RedirectAttributes redirect) {
		reviewService.changeStatus(id, ReviewStatus.REJECTED);
		redirect.addFlashAttribute("success", "Avaliação rejeitada — não aparece na loja.");
		return redirectTo(tab);
	}

	@PostMapping("/{id}/excluir")
	public String delete(@PathVariable Long id, @RequestParam(defaultValue = "pendentes") String tab,
			RedirectAttributes redirect) {
		reviewService.delete(id);
		redirect.addFlashAttribute("success", "Avaliação excluída.");
		return redirectTo(tab);
	}

	private static String redirectTo(String tab) {
		return "redirect:/admin/avaliacoes?status=" + (TABS.containsKey(tab) ? tab : "pendentes");
	}
}
