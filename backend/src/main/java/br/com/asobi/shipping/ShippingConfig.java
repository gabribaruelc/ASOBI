package br.com.asobi.shipping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShippingConfig {

	@Bean
	public ShippingQuoteProvider shippingQuoteProvider(MelhorEnvioProperties properties) {
		return new MelhorEnvioClient(properties);
	}
}
