package br.com.asobi.content.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PoliciesForm {

	private List<@Valid PolicyForm> policies = new ArrayList<>();

	public List<PolicyForm> getPolicies() {
		return policies;
	}

	public void setPolicies(List<PolicyForm> policies) {
		this.policies = policies;
	}

	public static class PolicyForm {

		private String slug;

		/** Só para exibir; o título não é editável. */
		private String title;

		@NotBlank(message = "Escreva o texto.")
		@Size(max = 20000, message = "Máximo de 20.000 caracteres.")
		private String body;

		public String getSlug() {
			return slug;
		}

		public void setSlug(String slug) {
			this.slug = slug;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}
	}
}
