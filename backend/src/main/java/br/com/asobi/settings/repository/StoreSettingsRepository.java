package br.com.asobi.settings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.settings.model.StoreSettings;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
}
