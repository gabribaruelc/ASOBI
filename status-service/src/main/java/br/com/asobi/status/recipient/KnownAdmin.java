package br.com.asobi.status.recipient;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cópia da última lista de admins lida, para avisar mesmo com o admin-auth-service fora do ar. */
@Entity
@Table(name = "known_admins")
public class KnownAdmin {

	@Id
	private String email;

	protected KnownAdmin() {
	}

	public KnownAdmin(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
