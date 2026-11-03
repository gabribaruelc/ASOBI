package br.com.asobi.admin.twofactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import br.com.asobi.admin.twofactor.AdminPasswordCheck.Result;

class AdminPasswordCheckTests {

	private static final String HASH = BCrypt.hashpw("certa", BCrypt.gensalt(4));

	private final MutableClock clock = new MutableClock();
	private final AdminPasswordCheck check = new AdminPasswordCheck(
			new AdminPasswordProperties("Admin@Asobi.com.br", HASH), clock);

	@Test
	void appliesOnlyToTheConfiguredAccount() {
		assertThat(check.appliesTo(" admin@asobi.com.br ")).isTrue();
		assertThat(check.appliesTo("outra@asobi.com.br")).isFalse();
		assertThat(new AdminPasswordCheck(new AdminPasswordProperties("admin@asobi.com.br", "")).appliesTo(
				"admin@asobi.com.br")).isFalse();
	}

	@Test
	void locksAfterFiveWrongTriesEvenForTheRightPassword() {
		for (int i = 1; i < AdminPasswordCheck.MAX_FAILURES; i++) {
			assertThat(check.check("errada")).isEqualTo(Result.WRONG);
		}
		assertThat(check.check("errada")).isEqualTo(Result.LOCKED);
		assertThat(check.check("certa")).isEqualTo(Result.LOCKED);

		clock.advance(AdminPasswordCheck.WINDOW.plusSeconds(1));
		assertThat(check.check("certa")).isEqualTo(Result.OK);
	}

	@Test
	void rightPasswordResetsTheCount() {
		check.check("errada");
		check.check("errada");
		assertThat(check.check("certa")).isEqualTo(Result.OK);
		for (int i = 1; i < AdminPasswordCheck.MAX_FAILURES; i++) {
			assertThat(check.check("errada")).isEqualTo(Result.WRONG);
		}
	}

	@Test
	void invalidHashStopsTheStartup() {
		assertThatThrownBy(() -> new AdminPasswordCheck(new AdminPasswordProperties("admin@asobi.com.br", "senha")))
				.isInstanceOf(IllegalStateException.class);
	}

	private static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-11-02T12:00:00Z");

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}
}
