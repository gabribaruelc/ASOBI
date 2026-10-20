package br.com.asobi.content.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.content.model.PolicyPage;

public interface PolicyPageRepository extends JpaRepository<PolicyPage, String> {

	List<PolicyPage> findAllByOrderByPositionAsc();
}
