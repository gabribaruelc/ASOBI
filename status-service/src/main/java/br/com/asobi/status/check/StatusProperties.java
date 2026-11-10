package br.com.asobi.status.check;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @param targets             o que conferir; endereço vazio = serviço ainda não está no ar, fica de fora
 * @param dependencies        nome no e-mail e o que fazer para cada parte informada pelo /actuator/health
 *                            dos serviços (db, resend...); a chave pode ser em kebab-case
 * @param timeout             tempo de espera de cada conferência (folga para o Cloud Run "acordar")
 * @param failuresBeforeAlert falhas seguidas antes de avisar, para não alarmar por um soluço
 * @param reminderInterval    se continuar fora do ar, avisa de novo depois deste tempo
 * @param storeUrl            endereço da loja, para o link do e-mail
 */
@ConfigurationProperties("status")
public record StatusProperties(List<Target> targets, Map<String, Dependency> dependencies, Duration timeout,
		int failuresBeforeAlert, Duration reminderInterval, String storeUrl) {

	public StatusProperties {
		targets = targets == null ? List.of() : targets;
		dependencies = dependencies == null ? Map.of() : dependencies;
	}

	public List<Target> activeTargets() {
		return targets.stream().filter(Target::isActive).toList();
	}

	/** "mercadoPago" (como vem do actuator) acha a chave "mercado-pago". */
	public Dependency dependency(String component) {
		String wanted = normalize(component);
		return dependencies.entrySet().stream()
				.filter(entry -> normalize(entry.getKey()).equals(wanted))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(new Dependency(component, null));
	}

	private static String normalize(String name) {
		return name.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
	}

	/**
	 * @param name  identificador fixo (fica no banco)
	 * @param label nome que aparece no e-mail
	 * @param url   endereço base do serviço
	 * @param path  caminho conferido (ex.: /actuator/health)
	 * @param hint  o que fazer quando cai (aparece no e-mail)
	 */
	public record Target(String name, String label, String url, String path, String hint) {

		public boolean isActive() {
			return StringUtils.hasText(url);
		}

		public String checkUrl() {
			String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
			return StringUtils.hasText(path) ? base + path : base;
		}
	}

	/**
	 * @param label nome que aparece no e-mail
	 * @param hint  o que fazer quando para
	 */
	public record Dependency(String label, String hint) {
	}
}
