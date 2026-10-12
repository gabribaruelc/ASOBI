package br.com.asobi.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Com RESEND_API_KEY definido, envia pelo Resend; sem ele, os e-mails só aparecem no log. */
@Configuration
public class EmailConfig {

	private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

	@Bean
	public EmailSender emailSender(EmailProperties properties) {
		if (StringUtils.hasText(properties.resendApiKey())) {
			log.info("E-mails: Resend (remetente {})", properties.from());
			return new ResendEmailSender(properties);
		}
		log.warn("E-mails: só no log (RESEND_API_KEY não definido).");
		return new LogEmailSender();
	}
}
