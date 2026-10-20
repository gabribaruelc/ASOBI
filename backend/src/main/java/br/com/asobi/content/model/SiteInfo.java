package br.com.asobi.content.model;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Contato e dados do rodapé. Linha única (id = 1). */
@Entity
@Table(name = "site_info")
public class SiteInfo {

	public static final long SINGLETON_ID = 1L;

	@Id
	private Long id;

	@Column(name = "contact_email", length = 254)
	private String contactEmail;

	@Column(name = "contact_phone", length = 30)
	private String contactPhone;

	@Column(name = "business_hours", length = 100)
	private String businessHours;

	@Column(name = "instagram_url", length = 200)
	private String instagramUrl;

	@Column(name = "facebook_url", length = 200)
	private String facebookUrl;

	@Column(name = "tiktok_url", length = 200)
	private String tiktokUrl;

	@Column(name = "legal_name", length = 150)
	private String legalName;

	@Column(length = 18)
	private String cnpj;

	@Column(length = 200)
	private String address;

	@Column(name = "updated_by_email", length = 254)
	private String updatedByEmail;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SiteInfo() {
	}

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
