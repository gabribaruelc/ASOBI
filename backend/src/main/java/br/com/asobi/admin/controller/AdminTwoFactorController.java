package br.com.asobi.admin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.admin.security.CurrentAdmin;
import br.com.asobi.admin.twofactor.TwoFactorGateway;
import br.com.asobi.admin.twofactor.TwoFactorGateway.Verification;
import br.com.asobi.admin.twofactor.TwoFactorSession;
import br.com.asobi.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Segundo passo do login do painel: o código enviado para o e-mail do admin. Enquanto ele não
 * é digitado, o SecurityConfig manda qualquer tela do painel para cá.
 */
@Controller
@RequestMapping("/admin/verificacao")
public class AdminTwoFactorController {

	private static final String PAGE = "redirect:/admin/verificacao";

	private final TwoFactorGateway gateway;
	private final TwoFactorSession twoFactorSession;

	public AdminTwoFactorController(TwoFactorGateway gateway, TwoFactorSession twoFactorSession) {
		this.gateway = gateway;
		this.twoFactorSession = twoFactorSession;
	}

	/**
	 * Na primeira visita já envia o código. Depois de um erro não reenvia sozinho: a pessoa
	 * pede pelo botão, para um recarregar de página não gastar o limite de envios.
	 */
	@GetMapping
	public String form(Authentication authentication, HttpServletRequest request, Model model) {
		String email = CurrentAdmin.emailOf(authentication);
		if (twoFactorSession.isSatisfied(request, email)) {
			return "redirect:/admin";
		}
		HttpSession session = request.getSession();
		if (twoFactorSession.challengeId(session) == null && !model.containsAttribute("error")) {
			try {
				twoFactorSession.requestFirstCode(session, email);
			} catch (BusinessException ex) {
				model.addAttribute("error", ex.getMessage());
			}
		}
		model.addAttribute("email", email);
		model.addAttribute("codeSent", twoFactorSession.challengeId(session) != null);
		return "admin/two-factor";
	}

	@PostMapping
	public String verify(@RequestParam String code, Authentication authentication, HttpServletRequest request,
			RedirectAttributes redirect) {
		String email = CurrentAdmin.emailOf(authentication);
		HttpSession session = request.getSession();
		String challengeId = twoFactorSession.challengeId(session);
		if (challengeId == null) {
			return PAGE;
		}
		String digits = code.replaceAll("\\s", "");
		if (!digits.matches("\\d{6}")) {
			redirect.addFlashAttribute("error", "O código tem 6 números.");
			return PAGE;
		}
		Verification verification = gateway.verify(challengeId, digits, email);
		switch (verification.status()) {
			case VERIFIED -> {
				twoFactorSession.markVerified(session, email);
				return "redirect:/admin";
			}
			case EXPIRED -> twoFactorSession.clearChallenge(session);
			case WRONG_CODE, UNAVAILABLE -> {
			}
		}
		redirect.addFlashAttribute("error", verification.message());
		return PAGE;
	}

	@PostMapping("/reenviar")
	public String resend(Authentication authentication, HttpServletRequest request, RedirectAttributes redirect) {
		HttpSession session = request.getSession();
		twoFactorSession.clearChallenge(session);
		try {
			twoFactorSession.setChallengeId(session, gateway.requestCode(CurrentAdmin.emailOf(authentication)));
			redirect.addFlashAttribute("success", "Enviamos um código novo.");
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return PAGE;
	}
}
