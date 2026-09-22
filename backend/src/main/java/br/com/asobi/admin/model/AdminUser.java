package br.com.asobi.admin.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** E-mail (conta Google) com acesso ao painel admin. */
@Entity
@Table(name = "admin_users")
public class AdminUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "created_by_email", length = 254)
	private String createdByEmail;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AdminUser() {
	}

	public AdminUser(String email, String createdByEmail) {
		this.email = email;
		this.createdByEmail = createdByEmail;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getCreatedByEmail() {
		return createdByEmail;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
