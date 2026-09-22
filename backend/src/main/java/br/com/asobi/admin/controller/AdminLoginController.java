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
import br.com.asobi.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class AdminLoginController {

	private final AdminProperties adminProperties;
	private final AdminUserService adminUserService;
	private final SecurityContextRepository securityContextRepository;
	private final boolean googleEnabled;

	public AdminLoginController(AdminProperties adminProperties, AdminUserService adminUserService,
			SecurityContextRepository securityContextRepository,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
		this.adminProperties = adminProperties;
		this.adminUserService = adminUserService;
		this.securityContextRepository = securityContextRepository;
		this.googleEnabled = clientRegistrations.getIfAvailable() != null;
	}

	@GetMapping("/admin/login")
	public String login(Model model) {
		model.addAttribute("googleEnabled", googleEnabled);
		model.addAttribute("devLoginEnabled", adminProperties.devLoginEnabled());
		return "admin/login";
	}

	/** Entrar só com o e-mail, sem Google. Existe apenas com asobi.admin.dev-login-enabled=true (perfil local). */
	@PostMapping("/admin/dev-login")
	public String devLogin(@RequestParam String email, HttpServletRequest request, HttpServletResponse response) {
		if (!adminProperties.devLoginEnabled()) {
			throw new ResponseStatusException(NOT_FOUND);
		}
		if (!adminUserService.isAdmin(email)) {
			return "redirect:/admin/login?error";
		}
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
				AdminUserService.normalize(email), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		// Nova sessão após o login (proteção contra session fixation).
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		securityContextRepository.saveContext(context, request, response);
		return "redirect:/admin";
	}
}
