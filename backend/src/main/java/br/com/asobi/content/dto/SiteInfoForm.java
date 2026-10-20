package br.com.asobi.content.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SiteInfoForm {

	private static final String LINK = "^$|^https://\\S+$";
	private static final String LINK_MESSAGE = "Cole o link completo, começando com https://";

	@Email(message = "E-mail inválido.")
	@Size(max = 254, message = "Máximo de 254 caracteres.")
	private String contactEmail;

	@Pattern(regexp = "^$|^[\\d\\s()+-]{10,30}$", message = "Telefone inválido. Ex.: (11) 91234-5678")
	private String contactPhone;

	@Size(max = 100, message = "Máximo de 100 caracteres.")
	private String businessHours;

	@Pattern(regexp = LINK, message = LINK_MESSAGE)
	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String instagramUrl;

	@Pattern(regexp = LINK, message = LINK_MESSAGE)
	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String facebookUrl;

	@Pattern(regexp = LINK, message = LINK_MESSAGE)
	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String tiktokUrl;

	@Size(max = 150, message = "Máximo de 150 caracteres.")
	private String legalName;

	@Pattern(regexp = "^$|^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$", message = "CNPJ inválido. Ex.: 12.345.678/0001-90")
	private String cnpj;

	@Size(max = 200, message = "Máximo de 200 caracteres.")
	private String address;

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getBusinessHours() {
		return businessHours;
	}

	public void setBusinessHours(String businessHours) {
		this.businessHours = businessHours;
	}

	public String getInstagramUrl() {
		return instagramUrl;
	}

	public void setInstagramUrl(String instagramUrl) {
		this.instagramUrl = instagramUrl;
	}

	public String getFacebookUrl() {
		return facebookUrl;
	}

	public void setFacebookUrl(String facebookUrl) {
		this.facebookUrl = facebookUrl;
	}

	public String getTiktokUrl() {
		return tiktokUrl;
	}

	public void setTiktokUrl(String tiktokUrl) {
		this.tiktokUrl = tiktokUrl;
	}

	public String getLegalName() {
		return legalName;
	}

	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}

	public String getCnpj() {
		return cnpj;
	}

	public void setCnpj(String cnpj) {
		this.cnpj = cnpj;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
