package br.com.asobi.admin.twofactor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conta do painel que confirma o login com senha em vez do código por e-mail.
 *
 * @param email conta que usa a senha (padrão admin@asobi.com.br)
 * @param hash  hash BCrypt da senha (ADMIN_PASSWORD_HASH); vazio = essa conta também recebe o código
 */
@ConfigurationProperties("asobi.admin-password")
public record AdminPasswordProperties(String email, String hash) {
}
