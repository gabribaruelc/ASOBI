package br.com.asobi.admin.twofactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class TwoFactorSessionTests {

	private static final String ADMIN = "admin@asobi.com.br";

	private final TwoFactorGateway gateway = mock(TwoFactorGateway.class);
	private final TwoFactorSession twoFactorSession = new TwoFactorSession(gateway);

	@Test
	void simultaneousFirstVisitsShareOneCode() throws Exception {
		CountDownLatch firstIsSending = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		given(gateway.requestCode(ADMIN)).willAnswer(invocation -> {
			firstIsSending.countDown();
			release.await(5, TimeUnit.SECONDS);
			return "challenge-1";
		}).willReturn("challenge-2");

		// Com a sessão no banco, cada requisição carrega a sua cópia: mesmo id, objetos diferentes.
		MockHttpSession oneTab = new MockHttpSession(null, "session-1");
		MockHttpSession otherTab = new MockHttpSession(null, "session-1");

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<String> first = executor.submit(() -> twoFactorSession.requestFirstCode(oneTab, ADMIN));
			assertThat(firstIsSending.await(5, TimeUnit.SECONDS)).isTrue();
			Future<String> second = executor.submit(() -> twoFactorSession.requestFirstCode(otherTab, ADMIN));
			release.countDown();

			assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("challenge-1");
			assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("challenge-1");
		} finally {
			executor.shutdownNow();
		}
		assertThat(twoFactorSession.challengeId(otherTab)).isEqualTo("challenge-1");
		verify(gateway, times(1)).requestCode(ADMIN);
	}

	@Test
	void askingAgainAfterClearingSendsAnotherCode() {
		given(gateway.requestCode(ADMIN)).willReturn("challenge-1", "challenge-2");
		MockHttpSession session = new MockHttpSession(null, "session-1");

		twoFactorSession.requestFirstCode(session, ADMIN);
		twoFactorSession.clearChallenge(session);

		assertThat(twoFactorSession.requestFirstCode(session, ADMIN)).isEqualTo("challenge-2");
	}

	@Test
	void sessionsDoNotShareCodes() {
		given(gateway.requestCode(ADMIN)).willReturn("challenge-1", "challenge-2");

		twoFactorSession.requestFirstCode(new MockHttpSession(null, "session-1"), ADMIN);

		assertThat(twoFactorSession.requestFirstCode(new MockHttpSession(null, "session-2"), ADMIN))
				.isEqualTo("challenge-2");
	}
}
