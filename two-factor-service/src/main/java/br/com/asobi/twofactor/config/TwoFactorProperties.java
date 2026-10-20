package br.com.asobi.twofactor.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param codeSecret      chave do HMAC dos códigos; vazia = chave aleatória a cada subida (só local)
 * @param codeTtl         validade de cada código
 * @param maxAttempts     erros permitidos por código
 * @param maxChallenges   códigos que o mesmo e-mail pode pedir dentro de {@code challengeWindow}
 * @param challengeWindow janela do limite acima
 * @param resendCooldown  pedidos repetidos dentro deste tempo recebem o mesmo código, sem novo e-mail
 */
@ConfigurationProperties("two-factor")
public record TwoFactorProperties(String codeSecret, Duration codeTtl, int maxAttempts, int maxChallenges,
		Duration challengeWindow, Duration resendCooldown) {
}
