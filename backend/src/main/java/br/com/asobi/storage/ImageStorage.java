package br.com.asobi.storage;

/**
 * Onde os arquivos de imagem ficam guardados. A chave é o caminho do arquivo
 * dentro do storage (ex.: {@code products/12/5f0c....jpg}).
 */
public interface ImageStorage {

	/** @throws StorageException se não der para gravar */
	void store(String key, byte[] content, String contentType);

	/** Não falha se o arquivo já não existir. */
	void delete(String key);

	/** Endereço público do arquivo, usado direto no {@code <img>} da loja. */
	String publicUrl(String key);
}
