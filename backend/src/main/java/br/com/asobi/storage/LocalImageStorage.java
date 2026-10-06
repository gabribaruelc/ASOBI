package br.com.asobi.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guarda as imagens numa pasta do disco, servida em /uploads/**. Só para
 * desenvolvimento: no Cloud Run o disco é apagado a cada nova instância.
 */
public class LocalImageStorage implements ImageStorage {

	public static final String URL_PATH = "/uploads/";

	private final Path baseDir;
	private final String publicUrl;

	public LocalImageStorage(String localDir, String publicUrl) {
		this.baseDir = Path.of(localDir).toAbsolutePath().normalize();
		this.publicUrl = publicUrl.replaceAll("/+$", "");
		try {
			Files.createDirectories(baseDir);
		} catch (IOException ex) {
			throw new StorageException("Não foi possível criar a pasta de imagens " + baseDir, ex);
		}
	}

	public Path baseDir() {
		return baseDir;
	}

	@Override
	public void store(String key, byte[] content, String contentType) {
		Path file = resolve(key);
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, content);
		} catch (IOException ex) {
			throw new StorageException("Falha ao gravar " + key, ex);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		} catch (IOException ex) {
			throw new StorageException("Falha ao apagar " + key, ex);
		}
	}

	@Override
	public String publicUrl(String key) {
		return publicUrl + URL_PATH + key;
	}

	private Path resolve(String key) {
		Path file = baseDir.resolve(key).normalize();
		if (!file.startsWith(baseDir)) {
			throw new IllegalArgumentException("Chave fora da pasta de imagens: " + key);
		}
		return file;
	}
}
