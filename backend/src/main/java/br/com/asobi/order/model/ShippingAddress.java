package br.com.asobi.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShippingAddress {

	@Column(name = "shipping_postal_code", nullable = false, length = 9)
	private String postalCode;

	@Column(name = "shipping_street", nullable = false, length = 200)
	private String street;

	@Column(name = "shipping_number", nullable = false, length = 20)
	private String number;

	@Column(name = "shipping_complement", length = 100)
	private String complement;

	@Column(name = "shipping_district", nullable = false, length = 100)
	private String district;

	@Column(name = "shipping_city", nullable = false, length = 100)
	private String city;

	@Column(name = "shipping_state", nullable = false, length = 2)
	private String state;

	protected ShippingAddress() {
	}

	public ShippingAddress(String postalCode, String street, String number, String complement, String district,
			String city, String state) {
		this.postalCode = postalCode;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.district = district;
		this.city = city;
		this.state = state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getStreet() {
		return street;
	}

	public String getNumber() {
		return number;
	}

	public String getComplement() {
		return complement;
	}

	public String getDistrict() {
		return district;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}
}
