package br.com.asobi.adminauth.admin;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	boolean existsByEmail(String email);

	List<AdminUser> findAllByOrderByEmailAsc();
}
