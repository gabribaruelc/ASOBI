package br.com.asobi.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param bootstrapEmail  primeiro admin, criado na subida se não houver nenhum
 * @param devLoginEnabled permite entrar só com o e-mail (sem Google); apenas desenvolvimento
 */
@ConfigurationProperties("asobi.admin")
public record AdminProperties(String bootstrapEmail, boolean devLoginEnabled) {
}
