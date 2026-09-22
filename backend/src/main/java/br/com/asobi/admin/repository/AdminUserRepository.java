package br.com.asobi.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.admin.model.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	boolean existsByEmail(String email);

	List<AdminUser> findAllByOrderByEmailAsc();
}
