package br.com.asobi.common.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Datas no fuso da loja (Brasília). */
public final class StoreTime {

	public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	private static final Duration ONE_DAY = Duration.ofDays(1);

	private StoreTime() {
	}

	/** "Até 30/09" inclui o dia 30 inteiro: termina 00:00 de 01/10. */
	public static Instant endOfDay(LocalDate date) {
		return date == null ? null : date.plusDays(1).atStartOfDay(ZONE).toInstant();
	}

	/** Inverso de {@link #endOfDay}. */
	public static LocalDate lastDay(Instant endExclusive) {
		return endExclusive == null ? null : LocalDate.ofInstant(endExclusive.minusSeconds(1), ZONE);
	}

	/** Arredonda para cima; 0 se já passou. */
	public static long daysRemaining(Instant end, Instant now) {
		if (end == null || !end.isAfter(now)) {
			return 0;
		}
		Duration left = Duration.between(now, end);
		return (left.toMillis() + ONE_DAY.toMillis() - 1) / ONE_DAY.toMillis();
	}
}
