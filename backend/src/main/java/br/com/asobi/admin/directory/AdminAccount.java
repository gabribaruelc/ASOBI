package br.com.asobi.admin.directory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminAccount(Long id, String email, String createdByEmail) {
}
