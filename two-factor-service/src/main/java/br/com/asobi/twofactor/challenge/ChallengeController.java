package br.com.asobi.twofactor.challenge;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.twofactor.challenge.ChallengeService.Verification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

	private final ChallengeService challengeService;

	public ChallengeController(ChallengeService challengeService) {
		this.challengeService = challengeService;
	}

	/** Gera um código e envia para o e-mail. A resposta nunca traz o código. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChallengeResponse create(@Validated @RequestBody ChallengeRequest request) {
		Challenge challenge = challengeService.create(request.email(), request.purpose());
		return new ChallengeResponse(challenge.getId(), challenge.getEmail(), challenge.getPurpose(),
				challenge.getExpiresAt());
	}

	@PostMapping("/{id}/verifications")
	public VerificationResponse verify(@PathVariable UUID id, @Validated @RequestBody VerificationRequest request) {
		Verification verification = challengeService.verify(id, request.code());
		Challenge challenge = verification.challenge();
		return switch (verification.status()) {
			case VERIFIED -> new VerificationResponse(challenge.getId(), challenge.getEmail(),
					challenge.getPurpose(), true);
			case WRONG_CODE -> throw new WrongCodeException("Código incorreto.", verification.attemptsLeft());
			case EXPIRED -> throw new ChallengeExpiredException("Este código expirou ou já foi usado. Peça um novo.");
		};
	}

	/** @param purpose para que o código serve (ex.: {@code admin-login}); um código só vale para a finalidade em que foi pedido */
	public record ChallengeRequest(
			@NotBlank(message = "Informe o e-mail.")
			@Email(message = "E-mail inválido.")
			@Size(max = 254, message = "E-mail longo demais.")
			String email,

			@NotBlank(message = "Informe a finalidade.")
			@Pattern(regexp = "[a-z0-9-]{1,40}", message = "Finalidade inválida.")
			String purpose) {

		public ChallengeRequest {
			email = email == null ? null : email.trim();
		}
	}

	public record ChallengeResponse(UUID id, String email, String purpose, Instant expiresAt) {
	}

	public record VerificationRequest(
			@NotBlank(message = "Informe o código.")
			@Pattern(regexp = "\\d{6}", message = "O código tem 6 números.")
			String code) {

		public VerificationRequest {
			code = code == null ? null : code.trim();
		}
	}

	public record VerificationResponse(UUID id, String email, String purpose, boolean verified) {
	}
}
