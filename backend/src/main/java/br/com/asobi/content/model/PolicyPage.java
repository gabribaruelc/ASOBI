package br.com.asobi.content.model;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Texto de uma política da loja. Os slugs são fixos (criados na migração). */
@Entity
@Table(name = "policy_pages")
public class PolicyPage {

	@Id
	@Column(length = 40)
	private String slug;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 20000)
	private String body;

	@Column(name = "updated_by_email", length = 254)
	private String updatedByEmail;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PolicyPage() {
	}

	public String getSlug() {
		return slug;
	}

	public int getPosition() {
		return position;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getUpdatedByEmail() {
		return updatedByEmail;
	}

	public void setUpdatedByEmail(String updatedByEmail) {
		this.updatedByEmail = updatedByEmail;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
