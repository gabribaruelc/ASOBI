package br.com.asobi.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Com MERCADO_PAGO_ACCESS_TOKEN definido, usa o Mercado Pago; sem ele, o modo
 * manual (a Priscila confirma os pagamentos no painel).
 */
@Configuration
public class PaymentConfig {

	private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

	@Bean
	public PaymentGateway paymentGateway(MercadoPagoProperties mercadoPago,
			@Value("${asobi.store-url}") String storeUrl,
			@Value("${asobi.public-url}") String publicUrl) {
		if (StringUtils.hasText(mercadoPago.accessToken())) {
			log.info("Pagamentos: Mercado Pago ({})",
					mercadoPago.accessToken().startsWith("TEST-") ? "sandbox" : "produção");
			return new MercadoPagoGateway(mercadoPago.accessToken(), storeUrl, publicUrl);
		}
		log.warn("Pagamentos: modo manual (MERCADO_PAGO_ACCESS_TOKEN não definido).");
		return new ManualPaymentGateway(storeUrl);
	}
}
