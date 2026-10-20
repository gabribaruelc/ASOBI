package br.com.asobi.adminauth.admin;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

	private final AdminUserRepository adminUserRepository;

	public AdminUserService(AdminUserRepository adminUserRepository) {
		this.adminUserRepository = adminUserRepository;
	}

	public static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	public boolean isAdmin(String email) {
		String normalized = normalize(email);
		return normalized != null && !normalized.isEmpty() && adminUserRepository.existsByEmail(normalized);
	}

	public List<AdminUser> listAdmins() {
		return adminUserRepository.findAllByOrderByEmailAsc();
	}

	public long countAdmins() {
		return adminUserRepository.count();
	}

	@Transactional
	public AdminUser addAdmin(String email, String createdByEmail) {
		String normalized = normalize(email);
		if (adminUserRepository.existsByEmail(normalized)) {
			throw new AdminConflictException("Esse e-mail já é admin.");
		}
		return adminUserRepository.save(new AdminUser(normalized, normalize(createdByEmail)));
	}

	/** Ninguém remove a si mesmo, para o painel nunca ficar sem admin. */
	@Transactional
	public void removeAdmin(Long id, String requestedByEmail) {
		AdminUser admin = adminUserRepository.findById(id)
				.orElseThrow(() -> new AdminNotFoundException("Admin não encontrado."));
		if (admin.getEmail().equals(normalize(requestedByEmail))) {
			throw new AdminConflictException("Você não pode remover o seu próprio acesso.");
		}
		adminUserRepository.delete(admin);
	}
}
