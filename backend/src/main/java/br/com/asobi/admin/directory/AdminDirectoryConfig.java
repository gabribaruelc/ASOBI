package br.com.asobi.admin.directory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import br.com.asobi.admin.AdminProperties;

@Configuration
public class AdminDirectoryConfig {

	private static final Logger log = LoggerFactory.getLogger(AdminDirectoryConfig.class);

	@Bean
	public AdminDirectory adminDirectory(AdminAuthServiceProperties properties, AdminProperties adminProperties) {
		if (StringUtils.hasText(properties.url())) {
			return new AdminAuthServiceClient(properties);
		}
		log.warn("ADMIN_AUTH_SERVICE_URL vazia: lista de admins em memória, só com ADMIN_BOOTSTRAP_EMAIL. "
				+ "Admins adicionados pelo painel somem quando o backend reinicia.");
		return new InMemoryAdminDirectory(adminProperties.bootstrapEmail());
	}
}
