package br.com.asobi.status.check;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetStateRepository extends JpaRepository<TargetState, String> {
}
