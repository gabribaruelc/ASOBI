package br.com.asobi.content.dto;

import br.com.asobi.content.model.SiteInfo;

/** @param whatsappUrl link wa.me montado a partir do telefone; null sem telefone */
public record SiteInfoResponse(
		String contactEmail,
		String contactPhone,
		String whatsappUrl,
		String businessHours,
		String instagramUrl,
		String facebookUrl,
		String tiktokUrl,
		String legalName,
		String cnpj,
		String address) {

	public static SiteInfoResponse from(SiteInfo info) {
		return new SiteInfoResponse(
				info.getContactEmail(),
				info.getContactPhone(),
				whatsappUrl(info.getContactPhone()),
				info.getBusinessHours(),
				info.getInstagramUrl(),
				info.getFacebookUrl(),
				info.getTiktokUrl(),
				info.getLegalName(),
				info.getCnpj(),
				info.getAddress());
	}

	static String whatsappUrl(String phone) {
		if (phone == null) {
			return null;
		}
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() < 10) {
			return null;
		}
		// DDD + número, sem o código do país
		return "https://wa.me/" + (digits.length() <= 11 ? "55" + digits : digits);
	}
}
