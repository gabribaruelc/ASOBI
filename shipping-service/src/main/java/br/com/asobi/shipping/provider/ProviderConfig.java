package br.com.asobi.shipping.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

	@Bean
	public MelhorEnvioClient shippingQuoteProvider(MelhorEnvioProperties properties) {
		return new MelhorEnvioClient(properties);
	}

	@Bean
	@ConditionalOnExpression("!'${melhor-envio.token:}'.isBlank()")
	public MelhorEnvioHealthIndicator melhorEnvioHealthIndicator(MelhorEnvioClient client) {
		return new MelhorEnvioHealthIndicator(client);
	}
}
