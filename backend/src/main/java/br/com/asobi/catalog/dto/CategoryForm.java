package br.com.asobi.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategoryForm {

	@NotBlank(message = "Informe o nome.")
	@Size(max = 100, message = "Máximo de 100 caracteres.")
	private String name;

	@NotNull(message = "Informe a ordem.")
	@Min(value = 0, message = "A ordem não pode ser negativa.")
	private Integer position = 0;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}
}
