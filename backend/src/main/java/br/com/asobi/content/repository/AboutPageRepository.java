package br.com.asobi.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.content.model.AboutPage;

public interface AboutPageRepository extends JpaRepository<AboutPage, Long> {
}
