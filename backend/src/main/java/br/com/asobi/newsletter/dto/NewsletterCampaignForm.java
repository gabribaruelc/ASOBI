package br.com.asobi.newsletter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NewsletterCampaignForm {

	@NotBlank(message = "Escreva o assunto.")
	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String subject;

	@NotBlank(message = "Escreva o texto.")
	@Size(max = 20000, message = "Máximo de 20.000 caracteres.")
	private String body;

	@Size(max = 60, message = "Máximo de 60 caracteres.")
	private String buttonLabel;

	@Size(max = 500, message = "Máximo de 500 caracteres.")
	@Pattern(regexp = "|https?://\\S+", message = "Use um endereço completo, começando com https://")
	private String buttonUrl;

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getButtonLabel() {
		return buttonLabel;
	}

	public void setButtonLabel(String buttonLabel) {
		this.buttonLabel = buttonLabel;
	}

	public String getButtonUrl() {
		return buttonUrl;
	}

	public void setButtonUrl(String buttonUrl) {
		this.buttonUrl = buttonUrl;
	}
}
