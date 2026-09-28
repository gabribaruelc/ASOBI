package br.com.asobi.shipping.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

	@Bean
	public ShippingQuoteProvider shippingQuoteProvider(MelhorEnvioProperties properties) {
		return new MelhorEnvioClient(properties);
	}
}
