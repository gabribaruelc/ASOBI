package br.com.asobi.shipping.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param token        vazio = cotação indisponível
 * @param contactEmail o Melhor Envio exige um e-mail no User-Agent
 */
@ConfigurationProperties("melhor-envio")
public record MelhorEnvioProperties(String token, boolean sandbox, String contactEmail) {
}
