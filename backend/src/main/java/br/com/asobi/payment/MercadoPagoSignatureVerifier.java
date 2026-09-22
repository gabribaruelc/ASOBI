package br.com.asobi.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.StringUtils;

/**
 * Confere a assinatura das notificações do Mercado Pago (header x-signature).
 * Manifesto: "id:{data.id};request-id:{x-request-id};ts:{ts};" assinado com
 * HMAC-SHA256 usando a assinatura secreta configurada no painel do Mercado Pago.
 */
public class MercadoPagoSignatureVerifier {

	private final byte[] secret;

	public MercadoPagoSignatureVerifier(String secret) {
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
	}

	public boolean isValid(String signatureHeader, String requestId, String dataId) {
		if (!StringUtils.hasText(signatureHeader)) {
			return false;
		}
		String ts = null;
		String v1 = null;
		for (String part : signatureHeader.split(",")) {
			String[] keyValue = part.trim().split("=", 2);
			if (keyValue.length != 2) {
				continue;
			}
			switch (keyValue[0].trim()) {
				case "ts" -> ts = keyValue[1].trim();
				case "v1" -> v1 = keyValue[1].trim();
				default -> {
				}
			}
		}
		if (ts == null || v1 == null) {
			return false;
		}

		StringBuilder manifest = new StringBuilder();
		if (StringUtils.hasText(dataId)) {
			manifest.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
		}
		if (StringUtils.hasText(requestId)) {
			manifest.append("request-id:").append(requestId).append(';');
		}
		manifest.append("ts:").append(ts).append(';');

		byte[] expected = hmacSha256(manifest.toString()).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
	}

	String hmacSha256(String message) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalStateException("HMAC-SHA256 indisponível", ex);
		}
	}
}
