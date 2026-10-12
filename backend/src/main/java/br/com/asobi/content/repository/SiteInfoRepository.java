package br.com.asobi.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.content.model.SiteInfo;

public interface SiteInfoRepository extends JpaRepository<SiteInfo, Long> {
}
