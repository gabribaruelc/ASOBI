package br.com.asobi.admin.twofactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwoFactorConfig {

	private static final Logger log = LoggerFactory.getLogger(TwoFactorConfig.class);

	@Bean
	public TwoFactorGateway twoFactorGateway(TwoFactorServiceProperties properties) {
		TwoFactorServiceClient client = new TwoFactorServiceClient(properties);
		if (client.isEnabled()) {
			log.info("Painel: código por e-mail depois do login (two-factor-service).");
		} else {
			log.warn("TWO_FACTOR_SERVICE_URL vazia: painel sem segundo fator.");
		}
		return client;
	}
}
