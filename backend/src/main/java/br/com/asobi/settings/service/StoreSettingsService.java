package br.com.asobi.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.settings.dto.StoreSettingsForm;
import br.com.asobi.settings.model.StoreSettings;
import br.com.asobi.settings.repository.StoreSettingsRepository;
import br.com.asobi.shipping.ShippingQuoteProvider;

@Service
@Transactional
public class StoreSettingsService {

	private final StoreSettingsRepository repository;
	private final ShippingQuoteProvider shippingQuoteProvider;

	public StoreSettingsService(StoreSettingsRepository repository, ShippingQuoteProvider shippingQuoteProvider) {
		this.repository = repository;
		this.shippingQuoteProvider = shippingQuoteProvider;
	}

	@Transactional(readOnly = true)
	public StoreSettings get() {
		return repository.findById(StoreSettings.SINGLETON_ID)
				.orElseThrow(() -> new NotFoundException("Configurações da loja não encontradas."));
	}

	@Transactional(readOnly = true)
	public StoreSettingsForm toForm() {
		StoreSettings settings = get();
		StoreSettingsForm form = new StoreSettingsForm();
		form.setShippingEnabled(settings.isShippingEnabled());
		form.setOriginPostalCode(settings.getOriginPostalCode());
		form.setFreeShippingThreshold(settings.getFreeShippingThreshold());
		form.setPackageWeightKg(settings.getPackageWeightKg());
		form.setPackageWidthCm(settings.getPackageWidthCm());
		form.setPackageHeightCm(settings.getPackageHeightCm());
		form.setPackageLengthCm(settings.getPackageLengthCm());
		return form;
	}

	public void update(StoreSettingsForm form, String adminEmail) {
		String postalCode = StringUtils.hasText(form.getOriginPostalCode())
				? formatPostalCode(form.getOriginPostalCode())
				: null;
		if (form.isShippingEnabled()) {
			if (!shippingQuoteProvider.isConfigured()) {
				throw new BusinessException(
						"Para ligar o frete automático, o serviço de frete precisa estar no ar (SHIPPING_SERVICE_URL) "
								+ "com o token do Melhor Envio configurado (MELHOR_ENVIO_TOKEN).");
			}
			if (postalCode == null) {
				throw new BusinessException("Informe o CEP de origem para ligar o frete automático.");
			}
		}
		StoreSettings settings = get();
		settings.setShippingEnabled(form.isShippingEnabled());
		settings.setOriginPostalCode(postalCode);
		settings.setFreeShippingThreshold(form.getFreeShippingThreshold());
		settings.setPackageWeightKg(form.getPackageWeightKg());
		settings.setPackageWidthCm(form.getPackageWidthCm());
		settings.setPackageHeightCm(form.getPackageHeightCm());
		settings.setPackageLengthCm(form.getPackageLengthCm());
		settings.setUpdatedByEmail(adminEmail);
	}

	public static String formatPostalCode(String postalCode) {
		String digits = postalCode.replaceAll("\\D", "");
		return digits.substring(0, 5) + "-" + digits.substring(5);
	}
}
