package br.com.asobi.status.email;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.status.check.StatusProperties;
import br.com.asobi.status.check.TargetState;

/** Monta os avisos aos admins (HTML do template email/alert.html + versão em texto). */
@Component
public class AlertEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR)
			.withZone(SAO_PAULO);

	private final ITemplateEngine templateEngine;
	private final String storeUrl;

	public AlertEmailComposer(ITemplateEngine templateEngine, StatusProperties properties) {
		this.templateEngine = templateEngine;
		this.storeUrl = properties.storeUrl();
	}

	/** @param detail o erro da última conferência (null quando voltou) */
	public record Incident(String label, String detail, Instant since, TargetState state) {
	}

	/** Uma linha do e-mail, já em texto. */
	public record Line(String label, String info) {
	}

	/** Os destinatários são preenchidos por quem envia. */
	public EmailMessage down(List<Incident> wentDown, List<Incident> stillDown, Instant now) {
		List<Line> lines = new ArrayList<>();
		wentDown.forEach(i -> lines.add(new Line(i.label(), downInfo(i, now))));
		stillDown.forEach(i -> lines.add(new Line(i.label(), "continua fora do ar · " + downInfo(i, now))));
		String subject = wentDown.isEmpty()
				? "⚠️ ASOBI: continua fora do ar — " + labels(stillDown)
				: "⚠️ ASOBI: fora do ar — " + labels(wentDown);
		return compose(subject, "down", "Algo saiu do ar",
				"A conferência automática não conseguiu falar com:",
				"Enquanto isso, a loja pode não abrir, não vender ou o painel pode não entrar. "
						+ "Se não voltar sozinho em alguns minutos, chame quem cuida da parte técnica.",
				lines);
	}

	public EmailMessage recovered(List<Incident> recovered, Instant now) {
		List<Line> lines = recovered.stream()
				.map(i -> new Line(i.label(), i.since() == null ? "voltou"
						: "voltou depois de " + duration(Duration.between(i.since(), now))))
				.toList();
		return compose("✅ ASOBI: voltou ao ar — " + labels(recovered), "up", "Tudo certo de novo",
				"Voltou a funcionar:", "Não precisa fazer nada.", lines);
	}

	private EmailMessage compose(String subject, String tone, String heading, String intro, String outro,
			List<Line> lines) {
		Context context = new Context(PT_BR);
		context.setVariable("tone", tone);
		context.setVariable("heading", heading);
		context.setVariable("intro", intro);
		context.setVariable("outro", outro);
		context.setVariable("lines", lines);
		context.setVariable("storeUrl", storeUrl);
		String text = heading + "\n\n" + intro + "\n"
				+ lines.stream().map(l -> "- " + l.label() + ": " + l.info()).collect(Collectors.joining("\n"))
				+ "\n\n" + outro;
		return new EmailMessage(List.of(), subject, templateEngine.process("email/alert", context), text);
	}

	private static String downInfo(Incident incident, Instant now) {
		String since = incident.since() == null ? ""
				: "desde " + WHEN.format(incident.since()) + " (" + duration(Duration.between(incident.since(), now))
						+ ")";
		if (incident.detail() == null) {
			return since;
		}
		return since.isEmpty() ? incident.detail() : incident.detail() + " · " + since;
	}

	private static String labels(List<Incident> incidents) {
		return incidents.stream().map(Incident::label).collect(Collectors.joining(", "));
	}

	static String duration(Duration duration) {
		long minutes = Math.max(1, duration.toMinutes());
		if (minutes < 60) {
			return minutes + " min";
		}
		long hours = minutes / 60;
		long rest = minutes % 60;
		return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
	}
}
