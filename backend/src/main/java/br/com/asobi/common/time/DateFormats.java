package br.com.asobi.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/** Usado nos templates: ${@dates.dateTime(x)}. */
@Component("dates")
public class DateFormats {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public String date(LocalDate date) {
		return date == null ? "" : DATE.format(date);
	}

	public String dateTime(Instant instant) {
		return instant == null ? "" : DATE_TIME.format(instant.atZone(StoreTime.ZONE));
	}
}
