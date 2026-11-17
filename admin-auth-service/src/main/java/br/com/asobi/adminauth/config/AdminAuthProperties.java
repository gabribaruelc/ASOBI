package br.com.asobi.adminauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param bootstrapEmail primeiro admin, criado na subida se a lista estiver vazia
 * @param mainEmail      conta principal: recolocada na subida se faltar e nunca removida
 */
@ConfigurationProperties("admin-auth")
public record AdminAuthProperties(String bootstrapEmail, String mainEmail, Google google) {

	/** @param clientId vazio = login Google indisponível */
	public record Google(String clientId) {
	}
}
