package br.com.asobi.twofactor.challenge;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import br.com.asobi.twofactor.config.TwoFactorProperties;

/**
 * HMAC-SHA256 do código, com chave que só o serviço conhece. Um hash simples não bastaria:
 * com só um milhão de códigos possíveis, quem lesse o banco descobriria todos por força bruta.
 * O id do desafio entra na conta para o mesmo código gerar hashes diferentes.
 */
@Component
public class CodeHasher {

	private static final Logger log = LoggerFactory.getLogger(CodeHasher.class);

	private static final String ALGORITHM = "HmacSHA256";

	private final SecretKeySpec key;

	public CodeHasher(TwoFactorProperties properties) {
		byte[] secret;
		if (StringUtils.hasText(properties.codeSecret())) {
			secret = properties.codeSecret().getBytes(StandardCharsets.UTF_8);
		} else {
			log.warn("TWO_FACTOR_CODE_SECRET vazia: usando uma chave aleatória. "
					+ "Os códigos pendentes deixam de valer quando o serviço reinicia.");
			secret = new byte[32];
			new SecureRandom().nextBytes(secret);
		}
		this.key = new SecretKeySpec(secret, ALGORITHM);
	}

	public String hash(UUID challengeId, String code) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(key);
			byte[] digest = mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HMAC-SHA256 indisponível", ex);
		}
	}

	public boolean matches(UUID challengeId, String code, String expectedHash) {
		return MessageDigest.isEqual(hash(challengeId, code).getBytes(StandardCharsets.US_ASCII),
				expectedHash.getBytes(StandardCharsets.US_ASCII));
	}
}
