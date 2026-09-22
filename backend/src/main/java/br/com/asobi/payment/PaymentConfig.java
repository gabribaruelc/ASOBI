package br.com.asobi.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

	@Bean
	public PaymentGateway paymentGateway(@Value("${asobi.store-url}") String storeUrl) {
		return new ManualPaymentGateway(storeUrl);
	}
}
