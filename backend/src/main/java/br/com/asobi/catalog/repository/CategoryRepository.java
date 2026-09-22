package br.com.asobi.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.asobi.catalog.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findAllByOrderByPositionAscNameAsc();

	Optional<Category> findBySlug(String slug);

	boolean existsBySlug(String slug);
}
