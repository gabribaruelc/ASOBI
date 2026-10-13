package br.com.asobi.adminauth.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
public class AdminController {

	private final AdminUserService adminUserService;

	public AdminController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping("/admins")
	public List<AdminResponse> list() {
		return adminUserService.listAdmins().stream().map(AdminResponse::from).toList();
	}

	@PostMapping("/admins")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminResponse add(@Validated @RequestBody AdminRequest request) {
		return AdminResponse.from(adminUserService.addAdmin(request.email(), request.createdByEmail()));
	}

	/** @param requestedBy e-mail de quem está removendo; ninguém remove a si mesmo */
	@DeleteMapping("/admins/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id, @RequestParam(required = false) String requestedBy) {
		adminUserService.removeAdmin(id, requestedBy);
	}

	/** O painel pergunta a cada requisição se o e-mail logado continua na lista. */
	@GetMapping("/authorizations")
	public AuthorizationResponse authorization(@RequestParam String email) {
		return new AuthorizationResponse(AdminUserService.normalize(email), adminUserService.isAdmin(email));
	}

	public record AdminRequest(
			@NotBlank(message = "Informe o e-mail.")
			@Email(message = "E-mail inválido.")
			@Size(max = 254, message = "E-mail longo demais.")
			String email,

			String createdByEmail) {

		public AdminRequest {
			email = email == null ? null : email.trim();
		}
	}

	public record AdminResponse(Long id, String email, String createdByEmail, Instant createdAt) {

		static AdminResponse from(AdminUser admin) {
			return new AdminResponse(admin.getId(), admin.getEmail(), admin.getCreatedByEmail(), admin.getCreatedAt());
		}
	}

	public record AuthorizationResponse(String email, boolean admin) {
	}
}
