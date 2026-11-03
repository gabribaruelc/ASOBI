package br.com.asobi.status.check;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @param targets             o que conferir; endereço vazio = serviço ainda não está no ar, fica de fora
 * @param timeout             tempo de espera de cada conferência (folga para o Cloud Run "acordar")
 * @param failuresBeforeAlert falhas seguidas antes de avisar, para não alarmar por um soluço
 * @param reminderInterval    se continuar fora do ar, avisa de novo depois deste tempo
 * @param storeUrl            endereço da loja, para o link do e-mail
 */
@ConfigurationProperties("status")
public record StatusProperties(List<Target> targets, Duration timeout, int failuresBeforeAlert,
		Duration reminderInterval, String storeUrl) {

	public StatusProperties {
		targets = targets == null ? List.of() : targets;
	}

	public List<Target> activeTargets() {
		return targets.stream().filter(Target::isActive).toList();
	}

	/**
	 * @param name  identificador fixo (fica no banco)
	 * @param label nome que aparece no e-mail
	 * @param url   endereço base do serviço
	 * @param path  caminho conferido (ex.: /actuator/health)
	 */
	public record Target(String name, String label, String url, String path) {

		public boolean isActive() {
			return StringUtils.hasText(url);
		}

		public String checkUrl() {
			String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
			return StringUtils.hasText(path) ? base + path : base;
		}
	}
}
