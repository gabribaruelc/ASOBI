package br.com.asobi.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductAlertConfig {

	private static final Logger log = LoggerFactory.getLogger(ProductAlertConfig.class);

	@Bean
	public ProductAlertGateway productAlertGateway(NotificationServiceProperties properties) {
		NotificationServiceClient client = new NotificationServiceClient(properties);
		if (!client.isEnabled()) {
			log.warn("NOTIFICATION_SERVICE_URL vazia: avisos de promoção e de volta ao estoque desligados.");
		}
		return client;
	}
}
