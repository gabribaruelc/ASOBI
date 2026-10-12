package br.com.asobi.content.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.content.dto.SiteInfoForm;
import br.com.asobi.content.dto.SiteInfoResponse;
import br.com.asobi.content.model.SiteInfo;
import br.com.asobi.content.repository.SiteInfoRepository;

@Service
@Transactional
public class SiteInfoService {

	private final SiteInfoRepository siteInfoRepository;

	public SiteInfoService(SiteInfoRepository siteInfoRepository) {
		this.siteInfoRepository = siteInfoRepository;
	}

	@Transactional(readOnly = true)
	public SiteInfoResponse getSiteInfo() {
		return SiteInfoResponse.from(load());
	}

	@Transactional(readOnly = true)
	public SiteInfoForm toForm() {
		SiteInfo info = load();
		SiteInfoForm form = new SiteInfoForm();
		form.setContactEmail(info.getContactEmail());
		form.setContactPhone(info.getContactPhone());
		form.setBusinessHours(info.getBusinessHours());
		form.setInstagramUrl(info.getInstagramUrl());
		form.setFacebookUrl(info.getFacebookUrl());
		form.setTiktokUrl(info.getTiktokUrl());
		form.setLegalName(info.getLegalName());
		form.setCnpj(info.getCnpj());
		form.setAddress(info.getAddress());
		return form;
	}

	public void update(SiteInfoForm form, String adminEmail) {
		SiteInfo info = load();
		info.setContactEmail(blankToNull(form.getContactEmail()));
		info.setContactPhone(blankToNull(form.getContactPhone()));
		info.setBusinessHours(blankToNull(form.getBusinessHours()));
		info.setInstagramUrl(blankToNull(form.getInstagramUrl()));
		info.setFacebookUrl(blankToNull(form.getFacebookUrl()));
		info.setTiktokUrl(blankToNull(form.getTiktokUrl()));
		info.setLegalName(blankToNull(form.getLegalName()));
		info.setCnpj(formatCnpj(form.getCnpj()));
		info.setAddress(blankToNull(form.getAddress()));
		info.setUpdatedByEmail(adminEmail);
	}

	private SiteInfo load() {
		return siteInfoRepository.findById(SiteInfo.SINGLETON_ID)
				.orElseThrow(() -> new NotFoundException("Dados de contato da loja não encontrados."));
	}

	/** Guarda sempre como 12.345.678/0001-90. */
	static String formatCnpj(String cnpj) {
		if (!StringUtils.hasText(cnpj)) {
			return null;
		}
		String digits = cnpj.replaceAll("\\D", "");
		return digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/"
				+ digits.substring(8, 12) + "-" + digits.substring(12);
	}

	private static String blankToNull(String text) {
		return StringUtils.hasText(text) ? text.trim() : null;
	}
}
