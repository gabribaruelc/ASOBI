package br.com.asobi.notification.subscription;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
import org.springframework.web.server.ResponseStatusException;

import br.com.asobi.notification.subscription.SubscriptionService.Subscribed;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	/** 201 para inscrição nova, 200 se a pessoa já tinha pedido esse aviso. */
	@PostMapping
	public ResponseEntity<SubscriptionResponse> subscribe(@Validated @RequestBody SubscriptionRequest request) {
		Subscribed subscribed = subscriptionService.subscribe(request.email(), request.productId(), request.type());
		return ResponseEntity.status(subscribed.created() ? HttpStatus.CREATED : HttpStatus.OK)
				.body(SubscriptionResponse.from(subscribed.subscription()));
	}

	@GetMapping
	public List<SubscriptionResponse> list(@RequestParam(required = false) String email,
			@RequestParam(required = false) Long productId) {
		return subscriptionService.list(email, productId).stream().map(SubscriptionResponse::from).toList();
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		subscriptionService.remove(id);
	}

	/** Apaga tudo de um e-mail (pedido de exclusão, LGPD) ou de um produto (saiu do catálogo). */
	@DeleteMapping
	public RemovalResponse removeAll(@RequestParam(required = false) String email,
			@RequestParam(required = false) Long productId) {
		boolean byEmail = StringUtils.hasText(email);
		if (byEmail == (productId != null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe email ou productId (só um dos dois).");
		}
		return new RemovalResponse(byEmail
				? subscriptionService.removeAllByEmail(email)
				: subscriptionService.removeAllByProduct(productId));
	}

	/** @param consent a pessoa marcou que quer receber o aviso; sem isso a inscrição é recusada */
	public record SubscriptionRequest(
			@NotBlank(message = "Informe o e-mail.")
			@Email(message = "E-mail inválido.")
			@Size(max = 254, message = "E-mail longo demais.")
			String email,

			@NotNull(message = "Informe o produto.")
			Long productId,

			@NotNull(message = "Informe o tipo de aviso.")
			AlertType type,

			@AssertTrue(message = "É preciso autorizar o envio do aviso.")
			boolean consent) {

		public SubscriptionRequest {
			email = email == null ? null : email.trim();
		}
	}

	public record SubscriptionResponse(Long id, String email, Long productId, AlertType type, Instant consentedAt,
			Instant lastNotifiedAt) {

		static SubscriptionResponse from(Subscription subscription) {
			return new SubscriptionResponse(subscription.getId(), subscription.getEmail(),
					subscription.getProductId(), subscription.getType(), subscription.getConsentedAt(),
					subscription.getLastNotifiedAt());
		}
	}

	public record RemovalResponse(int removed) {
	}
}
