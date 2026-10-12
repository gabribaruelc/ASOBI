package br.com.asobi.adminauth.login;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.adminauth.admin.AdminController.AuthorizationResponse;
import br.com.asobi.adminauth.admin.AdminUserService;
import br.com.asobi.adminauth.login.GoogleTokenVerifier.GoogleIdentity;
import jakarta.validation.constraints.NotBlank;

@RestController
public class LoginController {

	private final GoogleTokenVerifier tokenVerifier;
	private final AdminUserService adminUserService;

	public LoginController(GoogleTokenVerifier tokenVerifier, AdminUserService adminUserService) {
		this.tokenVerifier = tokenVerifier;
		this.adminUserService = adminUserService;
	}

	/**
	 * Login do painel com Google: o backend faz o redirecionamento e manda para cá o ID token
	 * que recebeu. Só passa token válido, com e-mail verificado e cadastrado como admin.
	 */
	@PostMapping("/api/logins")
	public AuthorizationResponse login(@Validated @RequestBody LoginRequest request) {
		GoogleIdentity identity = tokenVerifier.verify(request.idToken());
		if (!identity.emailVerified() || !adminUserService.isAdmin(identity.email())) {
			throw new NotAdminException("Este e-mail não tem acesso ao painel.");
		}
		return new AuthorizationResponse(AdminUserService.normalize(identity.email()), true);
	}

	public record LoginRequest(@NotBlank(message = "Informe o ID token do Google.") String idToken) {
	}
}
