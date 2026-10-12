package br.com.asobi.adminauth.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import br.com.asobi.adminauth.config.AdminAuthProperties;

/** Cria o primeiro admin (ADMIN_BOOTSTRAP_EMAIL) se a tabela estiver vazia. */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final AdminUserService adminUserService;
	private final AdminAuthProperties properties;

	public AdminBootstrap(AdminUserService adminUserService, AdminAuthProperties properties) {
		this.adminUserService = adminUserService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (adminUserService.countAdmins() > 0) {
			return;
		}
		if (!StringUtils.hasText(properties.bootstrapEmail())) {
			log.warn("Nenhum admin cadastrado. Defina ADMIN_BOOTSTRAP_EMAIL para liberar o primeiro acesso ao painel.");
			return;
		}
		adminUserService.addAdmin(properties.bootstrapEmail(), null);
		log.info("Primeiro admin criado: {}", AdminUserService.normalize(properties.bootstrapEmail()));
	}
}
