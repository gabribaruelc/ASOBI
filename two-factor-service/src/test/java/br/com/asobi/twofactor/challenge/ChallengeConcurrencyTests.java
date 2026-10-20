package br.com.asobi.twofactor.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.asobi.twofactor.email.EmailSender;

/** Sem @Transactional: cada pedido precisa ver o que o outro gravou. */
@SpringBootTest
@ActiveProfiles("test")
class ChallengeConcurrencyTests {

	private static final String EMAIL = "admin@asobi.com.br";

	@Autowired
	private ChallengeService challengeService;

	@Autowired
	private ChallengeRepository challengeRepository;

	@MockitoBean
	private EmailSender emailSender;

	@AfterEach
	void cleanUp() {
		challengeRepository.deleteAll();
	}

	@Test
	void simultaneousRequestsSendOneEmailAndShareTheCode() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		// o e-mail demora, como o Resend de verdade: os dois pedidos ficam em andamento juntos
		willAnswer(invocation -> {
			Thread.sleep(300);
			return null;
		}).given(emailSender).send(any());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Challenge> first = executor.submit(() -> {
				start.await();
				return challengeService.create(EMAIL, "admin-login");
			});
			Future<Challenge> second = executor.submit(() -> {
				start.await();
				return challengeService.create(EMAIL, "admin-login");
			});
			start.countDown();

			assertThat(first.get(10, TimeUnit.SECONDS).getId()).isEqualTo(second.get(10, TimeUnit.SECONDS).getId());
		} finally {
			executor.shutdownNow();
		}
		verify(emailSender, times(1)).send(any());
		assertThat(challengeRepository.count()).isOne();
	}
}
