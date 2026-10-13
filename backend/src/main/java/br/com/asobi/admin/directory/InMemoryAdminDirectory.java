package br.com.asobi.admin.directory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.StringUtils;

import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;

/**
 * Lista de admins em memória, para rodar o backend sem o admin-auth-service. Começa só com
 * o e-mail inicial e esquece o resto a cada reinício: desenvolvimento e testes.
 */
public class InMemoryAdminDirectory implements AdminDirectory {

	private final Map<Long, AdminAccount> admins = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);

	public InMemoryAdminDirectory(String bootstrapEmail) {
		if (StringUtils.hasText(bootstrapEmail)) {
			addAdmin(bootstrapEmail, null);
		}
	}

	@Override
	public boolean isAdmin(String email) {
		String normalized = AdminDirectory.normalize(email);
		return admins.values().stream().anyMatch(admin -> admin.email().equals(normalized));
	}

	/** O Spring Security do backend já conferiu o token; aqui só olha a lista. */
	@Override
	public boolean authorizeGoogleLogin(String idToken, String email) {
		return isAdmin(email);
	}

	@Override
	public List<AdminAccount> listAdmins() {
		return admins.values().stream().sorted(Comparator.comparing(AdminAccount::email)).toList();
	}

	@Override
	public synchronized AdminAccount addAdmin(String email, String createdByEmail) {
		if (isAdmin(email)) {
			throw new BusinessException("Esse e-mail já é admin.");
		}
		AdminAccount admin = new AdminAccount(nextId.getAndIncrement(), AdminDirectory.normalize(email),
				AdminDirectory.normalize(createdByEmail));
		admins.put(admin.id(), admin);
		return admin;
	}

	@Override
	public synchronized void removeAdmin(Long id, String currentEmail) {
		AdminAccount admin = admins.get(id);
		if (admin == null) {
			throw new NotFoundException("Admin não encontrado.");
		}
		if (admin.email().equals(AdminDirectory.normalize(currentEmail))) {
			throw new BusinessException("Você não pode remover o seu próprio acesso.");
		}
		admins.remove(id);
	}
}
