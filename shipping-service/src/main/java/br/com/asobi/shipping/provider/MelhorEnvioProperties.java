package br.com.asobi.shipping.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param token        token de acesso da conta Melhor Envio da ASOBI (vazio = cotação indisponível)
 * @param sandbox      true = ambiente de testes (sandbox.melhorenvio.com.br)
 * @param contactEmail e-mail técnico exigido pelo Melhor Envio no User-Agent
 */
@ConfigurationProperties("melhor-envio")
public record MelhorEnvioProperties(String token, boolean sandbox, String contactEmail) {
}
