package br.com.asobi.common.text;

import java.text.Normalizer;
import java.util.Locale;

/** Gera slugs de URL: "Missão no Castelo!" → "missao-no-castelo". */
public final class Slugs {

	private Slugs() {
	}

	public static String slugify(String text) {
		if (text == null) {
			return "";
		}
		String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return withoutAccents.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-+|-+$)", "");
	}
}
