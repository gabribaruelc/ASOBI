package br.com.asobi.order.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.asobi.order.service.PaymentReminderService;

/**
 * Rotina chamada pelo Cloud Scheduler (de hora em hora), com a chave {@code JOBS_API_KEY} no
 * cabeçalho X-Job-Key. Sem a chave configurada, a rota não existe.
 */
@RestController
public class PaymentReminderJobController {

	public static final String HEADER = "X-Job-Key";

	private final PaymentReminderService reminderService;
	private final byte[] apiKey;

	public PaymentReminderJobController(PaymentReminderService reminderService,
			@Value("${asobi.jobs.api-key}") String apiKey) {
		this.reminderService = reminderService;
		this.apiKey = StringUtils.hasText(apiKey) ? apiKey.getBytes(StandardCharsets.UTF_8) : null;
	}

	@PostMapping("/api/jobs/payment-reminders")
	public JobResult sendPaymentReminders(@RequestHeader(name = HEADER, required = false) String key) {
		if (apiKey == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (key == null || !MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8))) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return new JobResult(reminderService.sendDue());
	}

	public record JobResult(int sent) {
	}
}
