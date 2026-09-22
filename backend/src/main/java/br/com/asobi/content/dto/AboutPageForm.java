package br.com.asobi.content.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AboutPageForm {

	@NotBlank(message = "Escreva a história da ASOBI.")
	@Size(max = 4000, message = "Máximo de 4000 caracteres.")
	private String heroText;

	private List<@Valid ValueForm> values = new ArrayList<>();

	@Size(max = 16, message = "Use só um emoji.")
	private String missionEmoji;

	@NotBlank(message = "Informe o título da missão.")
	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String missionTitle;

	@NotBlank(message = "Escreva o texto da missão.")
	@Size(max = 4000, message = "Máximo de 4000 caracteres.")
	private String missionText;

	public String getHeroText() {
		return heroText;
	}

	public void setHeroText(String heroText) {
		this.heroText = heroText;
	}

	public List<ValueForm> getValues() {
		return values;
	}

	public void setValues(List<ValueForm> values) {
		this.values = values;
	}

	public String getMissionEmoji() {
		return missionEmoji;
	}

	public void setMissionEmoji(String missionEmoji) {
		this.missionEmoji = missionEmoji;
	}

	public String getMissionTitle() {
		return missionTitle;
	}

	public void setMissionTitle(String missionTitle) {
		this.missionTitle = missionTitle;
	}

	public String getMissionText() {
		return missionText;
	}

	public void setMissionText(String missionText) {
		this.missionText = missionText;
	}

	public static class ValueForm {

		@Size(max = 16, message = "Use só um emoji.")
		private String icon;

		@NotBlank(message = "Informe o título.")
		@Size(max = 100, message = "Máximo de 100 caracteres.")
		private String title;

		@NotBlank(message = "Escreva o texto.")
		@Size(max = 1000, message = "Máximo de 1000 caracteres.")
		private String text;

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
