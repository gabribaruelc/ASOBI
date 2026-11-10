package br.com.asobi.status.check;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.asobi.status.check.HealthProber.Probe;
import br.com.asobi.status.check.StatusProperties.Dependency;
import br.com.asobi.status.check.StatusProperties.Target;
import br.com.asobi.status.email.AlertEmailComposer;
import br.com.asobi.status.email.AlertEmailComposer.Incident;
import br.com.asobi.status.email.EmailMessage;
import br.com.asobi.status.email.EmailSender;
import br.com.asobi.status.recipient.AlertRecipients;

/**
 * Uma rodada de conferência: testa todos os serviços ao mesmo tempo (e as partes que cada um
 * informa, como banco e Resend), guarda o resultado e manda no máximo dois e-mails aos admins
 * (o que parou / o que voltou). Um item só é dado como parado depois de
 * {@code failuresBeforeAlert} falhas seguidas.
 */
@Service
public class CheckService {

	private static final Logger log = LoggerFactory.getLogger(CheckService.class);

	/** Separa no banco as partes (db, resend...) dos serviços conferidos. */
	private static final String DEPENDENCY_PREFIX = "dependency:";

	private final StatusProperties properties;
	private final HealthProber prober;
	private final TargetStateRepository repository;
	private final AlertRecipients recipients;
	private final AlertEmailComposer composer;
	private final EmailSender emailSender;
	private final Clock clock;

	public CheckService(StatusProperties properties, HealthProber prober, TargetStateRepository repository,
			AlertRecipients recipients, AlertEmailComposer composer, EmailSender emailSender, Clock clock) {
		this.properties = properties;
		this.prober = prober;
		this.repository = repository;
		this.recipients = recipients;
		this.composer = composer;
		this.emailSender = emailSender;
		this.clock = clock;
	}

	public record TargetResult(String name, String label, boolean up, String detail) {
	}

	/** Um item conferido: um serviço ou uma parte que ele usa (banco, Resend...). */
	private record Check(String name, String label, String hint, boolean up, String detail) {
	}

	/** Uma rodada por vez (o Cloud Scheduler pode repetir a chamada se ela demorar). */
	public synchronized List<TargetResult> run() {
		List<Target> targets = properties.activeTargets();
		Map<Target, Probe> probes = probeAll(targets);
		recipients.refresh();

		List<Check> checks = new ArrayList<>();
		for (Target target : targets) {
			Probe probe = probes.get(target);
			checks.add(new Check(target.name(), target.label(), target.hint(), probe.up(), probe.detail()));
		}
		checks.addAll(dependencyChecks(targets, probes));

		Instant now = clock.instant();
		List<Incident> wentDown = new ArrayList<>();
		List<Incident> stillDown = new ArrayList<>();
		List<Incident> recovered = new ArrayList<>();
		List<TargetResult> results = new ArrayList<>();

		for (Check check : checks) {
			TargetState state = repository.findById(check.name()).orElseGet(() -> new TargetState(check.name()));
			if (check.up()) {
				state.recordUp(now);
				if (state.getAlertedAt() != null) {
					recovered.add(new Incident(check.label(), null, null, state.getDownSince(), state));
				}
			} else {
				state.recordDown(now, check.detail());
				if (state.getConsecutiveFailures() >= properties.failuresBeforeAlert()) {
					Incident incident = new Incident(check.label(), check.detail(), check.hint(), state.getDownSince(),
							state);
					if (state.getAlertedAt() == null) {
						wentDown.add(incident);
					} else if (!state.getAlertedAt().plus(properties.reminderInterval()).isAfter(now)) {
						stillDown.add(incident);
					}
				}
			}
			repository.save(state);
			results.add(new TargetResult(check.name(), check.label(), check.up(), check.detail()));
		}

		if (!wentDown.isEmpty() || !stillDown.isEmpty()) {
			List<Incident> alerted = new ArrayList<>(wentDown);
			alerted.addAll(stillDown);
			if (send(composer.down(wentDown, stillDown, now))) {
				alerted.forEach(incident -> incident.state().markAlerted(now));
				repository.saveAll(alerted.stream().map(Incident::state).toList());
			}
		}
		if (!recovered.isEmpty() && send(composer.recovered(recovered, now))) {
			recovered.forEach(incident -> incident.state().markRecoveryNotified());
			repository.saveAll(recovered.stream().map(Incident::state).toList());
		}
		return results;
	}

	/**
	 * As partes que os serviços informam no /actuator/health. A mesma parte em vários serviços
	 * (o banco, por exemplo) vira um item só, parado se algum deles diz que está parada.
	 * Serviço que não respondeu não informa nada, e a parte fica como estava.
	 */
	private List<Check> dependencyChecks(List<Target> targets, Map<Target, Probe> probes) {
		Map<String, List<String>> reportedDownBy = new LinkedHashMap<>();
		for (Target target : targets) {
			probes.get(target).components().forEach((component, status) -> {
				if ("UP".equals(status)) {
					reportedDownBy.putIfAbsent(component, new ArrayList<>());
				} else if ("DOWN".equals(status) || "OUT_OF_SERVICE".equals(status)) {
					reportedDownBy.computeIfAbsent(component, key -> new ArrayList<>()).add(target.label());
				}
			});
		}
		List<Check> checks = new ArrayList<>();
		reportedDownBy.forEach((component, downBy) -> {
			Dependency dependency = properties.dependency(component);
			boolean up = downBy.isEmpty();
			checks.add(new Check(DEPENDENCY_PREFIX + component, dependency.label(), dependency.hint(), up,
					up ? null : "parou de funcionar (afeta: " + String.join(", ", downBy) + ")"));
		});
		return checks;
	}

	private Map<Target, Probe> probeAll(List<Target> targets) {
		Map<Target, CompletableFuture<Probe>> futures = new LinkedHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, targets.size()));
		try {
			for (Target target : targets) {
				futures.put(target, CompletableFuture.supplyAsync(
						() -> prober.probe(target.checkUrl(), properties.timeout()), executor));
			}
			Map<Target, Probe> probes = new LinkedHashMap<>();
			futures.forEach((target, future) -> probes.put(target, future.join()));
			return probes;
		} finally {
			executor.shutdown();
		}
	}

	/** Sem enviar, nada é marcado como avisado: a próxima rodada tenta de novo. */
	private boolean send(EmailMessage message) {
		List<String> to = recipients.current();
		if (to.isEmpty()) {
			log.error("Aviso não enviado: nenhum admin conhecido e STATUS_FALLBACK_EMAILS vazio. {}", message.subject());
			return false;
		}
		try {
			emailSender.send(new EmailMessage(to, message.subject(), message.html(), message.text()));
			return true;
		} catch (RuntimeException ex) {
			log.error("Falha ao enviar o aviso \"{}\"; tenta de novo na próxima rodada", message.subject(), ex);
			return false;
		}
	}
}
