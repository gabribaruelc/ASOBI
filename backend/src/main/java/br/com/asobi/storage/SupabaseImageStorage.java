package br.com.asobi.storage;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Supabase Storage pela API REST, num bucket público: o backend grava com a chave
 * secreta e a loja lê direto pela URL pública (sem passar pelo Cloud Run).
 */
public class SupabaseImageStorage implements ImageStorage {

	private static final Logger log = LoggerFactory.getLogger(SupabaseImageStorage.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	/** Os nomes de arquivo nunca se repetem, então o navegador pode guardar a foto por um ano. */
	private static final String CACHE_CONTROL = "max-age=31536000";

	private final RestClient restClient;
	private final String baseUrl;
	private final String bucket;

	public SupabaseImageStorage(StorageProperties properties) {
		this(properties, RestClient.builder().requestFactory(timeoutRequestFactory()));
	}

	/** Construtor para testes (MockRestServiceServer). */
	SupabaseImageStorage(StorageProperties properties, RestClient.Builder builder) {
		this.baseUrl = properties.supabaseUrl().trim().replaceAll("/+$", "");
		this.bucket = properties.bucket();
		this.restClient = builder
				.baseUrl(baseUrl + "/storage/v1")
				.defaultHeader("apikey", properties.supabaseKey())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.supabaseKey())
				.build();
	}

	private static JdkClientHttpRequestFactory timeoutRequestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}

	@Override
	public void store(String key, byte[] content, String contentType) {
		try {
			upload(key, content, contentType);
		} catch (RestClientResponseException ex) {
			if (!isBucketMissing(ex)) {
				throw failure("gravar", key, ex);
			}
			// Primeira foto do projeto: cria o bucket e tenta de novo.
			try {
				createBucket();
				upload(key, content, contentType);
			} catch (RestClientException retryEx) {
				throw failure("gravar", key, retryEx);
			}
		} catch (RestClientException ex) {
			throw failure("gravar", key, ex);
		}
	}

	@Override
	public void delete(String key) {
		try {
			restClient.delete().uri("/object/" + bucket + "/" + key).retrieve().toBodilessEntity();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() != 404 && !ex.getResponseBodyAsString().contains("not_found")) {
				throw failure("apagar", key, ex);
			}
		} catch (RestClientException ex) {
			throw failure("apagar", key, ex);
		}
	}

	@Override
	public String publicUrl(String key) {
		return baseUrl + "/storage/v1/object/public/" + bucket + "/" + key;
	}

	private void upload(String key, byte[] content, String contentType) {
		restClient.post()
				.uri("/object/" + bucket + "/" + key)
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
				.body(content)
				.retrieve()
				.toBodilessEntity();
	}

	private void createBucket() {
		log.info("Criando o bucket público \"{}\" no Supabase Storage", bucket);
		restClient.post()
				.uri("/bucket")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("id", bucket, "name", bucket, "public", true))
				.retrieve()
				.toBodilessEntity();
	}

	private static boolean isBucketMissing(RestClientResponseException ex) {
		return ex.getResponseBodyAsString().contains("Bucket not found");
	}

	private static StorageException failure(String action, String key, RestClientException ex) {
		String detail = ex instanceof RestClientResponseException response
				? response.getStatusCode() + " " + response.getResponseBodyAsString()
				: ex.getMessage();
		return new StorageException("Supabase Storage: falha ao " + action + " " + key + " (" + detail + ")", ex);
	}
}
