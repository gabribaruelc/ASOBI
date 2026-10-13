package br.com.asobi.adminauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param bootstrapEmail primeiro admin, criado na subida se a lista estiver vazia */
@ConfigurationProperties("admin-auth")
public record AdminAuthProperties(String bootstrapEmail, Google google) {

	/** @param clientId vazio = login Google indisponível */
	public record Google(String clientId) {
	}
}
