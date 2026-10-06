package br.com.asobi.storage;

/** Falha ao gravar ou apagar um arquivo no storage. */
public class StorageException extends RuntimeException {

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
