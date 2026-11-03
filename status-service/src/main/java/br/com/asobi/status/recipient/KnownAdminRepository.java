package br.com.asobi.status.recipient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnownAdminRepository extends JpaRepository<KnownAdmin, String> {
}
