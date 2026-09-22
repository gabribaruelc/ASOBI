package br.com.asobi.content.dto;

import java.util.List;

import br.com.asobi.content.model.AboutPage;

/** Mesmo formato que a loja já usa (SiteContentContext). */
public record AboutPageResponse(
		String heroText,
		List<Value> values,
		String missionEmoji,
		String missionTitle,
		String missionText) {

	public record Value(String icon, String title, String text) {
	}

	public static AboutPageResponse from(AboutPage page) {
		return new AboutPageResponse(
				page.getHeroText(),
				page.getValues().stream().map(v -> new Value(v.getIcon(), v.getTitle(), v.getText())).toList(),
				page.getMissionEmoji(),
				page.getMissionTitle(),
				page.getMissionText());
	}
}
