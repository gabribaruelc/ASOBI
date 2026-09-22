package br.com.asobi.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Faixa etária usada para organizar o catálogo (ex.: "4 a 6 anos"). */
@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String slug;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private int position;

	protected Category() {
	}

	public Category(String slug, String name, int position) {
		this.slug = slug;
		this.name = name;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
