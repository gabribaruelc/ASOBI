package br.com.asobi.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import br.com.asobi.admin.AdminProperties;

/**
 * Cria o primeiro admin (ADMIN_BOOTSTRAP_EMAIL) quando a tabela está vazia.
 * Depois disso, os demais admins são cadastrados pelo próprio painel.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final AdminUserService adminUserService;
	private final AdminProperties adminProperties;

	public AdminBootstrap(AdminUserService adminUserService, AdminProperties adminProperties) {
		this.adminUserService = adminUserService;
		this.adminProperties = adminProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (adminUserService.countAdmins() > 0) {
			return;
		}
		if (!StringUtils.hasText(adminProperties.bootstrapEmail())) {
			log.warn("Nenhum admin cadastrado. Defina ADMIN_BOOTSTRAP_EMAIL para liberar o primeiro acesso ao painel.");
			return;
		}
		adminUserService.addAdmin(adminProperties.bootstrapEmail(), null);
		log.info("Primeiro admin criado: {}", AdminUserService.normalize(adminProperties.bootstrapEmail()));
	}
}
