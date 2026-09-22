package br.com.asobi.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Adulto responsável pela compra. */
@Embeddable
public class Customer {

	@Column(name = "customer_name", nullable = false, length = 120)
	private String name;

	@Column(name = "customer_email", nullable = false, length = 254)
	private String email;

	@Column(name = "customer_phone", nullable = false, length = 30)
	private String phone;

	protected Customer() {
	}

	public Customer(String name, String email, String phone) {
		this.name = name;
		this.email = email;
		this.phone = phone;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	/** Link wa.me para falar com o cliente (número brasileiro, com DDD). */
	public String getWhatsappUrl() {
		String digits = phone == null ? "" : phone.replaceAll("\\D", "");
		if (digits.startsWith("55") && digits.length() > 11) {
			digits = digits.substring(2);
		}
		return "https://wa.me/55" + digits;
	}
}
