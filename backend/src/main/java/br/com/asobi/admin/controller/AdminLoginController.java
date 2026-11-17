package br.com.asobi.admin.controller;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import br.com.asobi.admin.AdminProperties;
import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.admin.twofactor.AdminPasswordCheck;
import br.com.asobi.admin.twofactor.TwoFactorSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class AdminLoginController {

	private final AdminProperties adminProperties;
	private final AdminDirectory adminDirectory;
	private final SecurityContextRepository securityContextRepository;
	private final AdminPasswordCheck passwordCheck;
	private final TwoFactorSession twoFactorSession;
	private final boolean googleEnabled;

	public AdminLoginController(AdminProperties adminProperties, AdminDirectory adminDirectory,
			SecurityContextRepository securityContextRepository, AdminPasswordCheck passwordCheck,
			TwoFactorSession twoFactorSession, ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
		this.adminProperties = adminProperties;
		this.adminDirectory = adminDirectory;
		this.securityContextRepository = securityContextRepository;
		this.passwordCheck = passwordCheck;
		this.twoFactorSession = twoFactorSession;
		this.googleEnabled = clientRegistrations.getIfAvailable() != null;
	}

	@GetMapping("/admin/login")
	public String login(Model model) {
		model.addAttribute("googleEnabled", googleEnabled);
		model.addAttribute("devLoginEnabled", adminProperties.devLoginEnabled());
		model.addAttribute("passwordLoginEnabled", passwordCheck.isEnabled());
		return "admin/login";
	}

	/**
	 * Conta principal (admin@asobistore.app): entra com e-mail e senha, sem Google. A senha já é a
	 * confirmação, então o código por e-mail não é pedido. Qualquer outro e-mail recebe o mesmo
	 * "e-mail ou senha incorretos", para a tela não revelar qual é a conta principal.
	 */
	@PostMapping("/admin/login/senha")
	public String passwordLogin(@RequestParam String email, @RequestParam String password,
			HttpServletRequest request, HttpServletResponse response) {
		if (!passwordCheck.appliesTo(email)) {
			return "redirect:/admin/login?senha";
		}
		AdminPasswordCheck.Result result = passwordCheck.check(password);
		if (result == AdminPasswordCheck.Result.LOCKED) {
			return "redirect:/admin/login?bloqueado";
		}
		if (result != AdminPasswordCheck.Result.OK) {
			return "redirect:/admin/login?senha";
		}
		if (!adminDirectory.isAdmin(email)) {
			return "redirect:/admin/login?error";
		}
		signIn(email, request, response);
		twoFactorSession.markVerified(request.getSession(), email);
		return "redirect:/admin";
	}

	/** Login sem Google, só no perfil local (asobi.admin.dev-login-enabled=true). */
	@PostMapping("/admin/dev-login")
	public String devLogin(@RequestParam String email, HttpServletRequest request, HttpServletResponse response) {
		if (!adminProperties.devLoginEnabled()) {
			throw new ResponseStatusException(NOT_FOUND);
		}
		if (!adminDirectory.isAdmin(email)) {
			return "redirect:/admin/login?error";
		}
		signIn(email, request, response);
		return "redirect:/admin";
	}

	private void signIn(String email, HttpServletRequest request, HttpServletResponse response) {
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				AdminDirectory.normalize(email), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		// troca a sessão no login, para ninguém aproveitar uma sessão criada antes (ataque "session fixation")
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		securityContextRepository.saveContext(context, request, response);
	}
}
