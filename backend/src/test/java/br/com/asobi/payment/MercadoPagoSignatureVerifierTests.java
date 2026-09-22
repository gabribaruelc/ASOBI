package br.com.asobi.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MercadoPagoSignatureVerifierTests {

	private final MercadoPagoSignatureVerifier verifier = new MercadoPagoSignatureVerifier("segredo-de-teste");

	@Test
	void acceptsSignatureBuiltFromManifest() {
		String v1 = verifier.hmacSha256("id:123456;request-id:req-1;ts:1704908010;");
		assertThat(verifier.isValid("ts=1704908010,v1=" + v1, "req-1", "123456")).isTrue();
	}

	@Test
	void lowercasesAlphanumericDataId() {
		String v1 = verifier.hmacSha256("id:abc123;request-id:req-1;ts:1;");
		assertThat(verifier.isValid("ts=1, v1=" + v1, "req-1", "ABC123")).isTrue();
	}

	@Test
	void rejectsTamperedValues() {
		String v1 = verifier.hmacSha256("id:123456;request-id:req-1;ts:1704908010;");
		assertThat(verifier.isValid("ts=1704908010,v1=" + v1, "req-1", "999999")).isFalse();
		assertThat(verifier.isValid("ts=1704908011,v1=" + v1, "req-1", "123456")).isFalse();
	}

	@Test
	void rejectsMissingOrMalformedHeader() {
		assertThat(verifier.isValid(null, "req-1", "123")).isFalse();
		assertThat(verifier.isValid("", "req-1", "123")).isFalse();
		assertThat(verifier.isValid("ts=1", "req-1", "123")).isFalse();
		assertThat(verifier.isValid("lixo", "req-1", "123")).isFalse();
	}

	@Test
	void rejectsSignatureFromAnotherSecret() {
		String v1 = new MercadoPagoSignatureVerifier("outro-segredo").hmacSha256("id:1;request-id:r;ts:1;");
		assertThat(verifier.isValid("ts=1,v1=" + v1, "r", "1")).isFalse();
	}
}
