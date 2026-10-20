package br.com.asobi.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param bootstrapEmail  único admin quando o backend roda sem o admin-auth-service
 * @param devLoginEnabled permite entrar só com o e-mail (sem Google); apenas desenvolvimento
 */
@ConfigurationProperties("asobi.admin")
public record AdminProperties(String bootstrapEmail, boolean devLoginEnabled) {
}
