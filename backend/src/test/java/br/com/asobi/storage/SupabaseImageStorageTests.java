package br.com.asobi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseImageStorageTests {

	private static final String BASE = "https://abc.supabase.co";
	private static final String KEY = "products/7/foto.jpg";
	private static final String OBJECT_URL = BASE + "/storage/v1/object/product-images/" + KEY;
	private static final byte[] CONTENT = { 1, 2, 3 };
	private static final String BUCKET_NOT_FOUND = """
			{"statusCode": "404", "error": "Bucket not found", "message": "Bucket not found"}
			""";

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final SupabaseImageStorage storage = new SupabaseImageStorage(
			new StorageProperties(BASE + "/", "secret-key", "product-images", null), builder);

	@Test
	void uploadsWithSecretKeyAndLongCache() {
		server.expect(requestTo(OBJECT_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("apikey", "secret-key"))
				.andExpect(header("Authorization", "Bearer secret-key"))
				.andExpect(header("Content-Type", "image/jpeg"))
				.andExpect(header("Cache-Control", "max-age=31536000"))
				.andExpect(content().bytes(CONTENT))
				.andRespond(withSuccess("{\"Key\": \"product-images/" + KEY + "\"}", MediaType.APPLICATION_JSON));

		storage.store(KEY, CONTENT, "image/jpeg");

		server.verify();
	}

	@Test
	void createsPublicBucketOnFirstUpload() {
		server.expect(requestTo(OBJECT_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON).body(BUCKET_NOT_FOUND));
		server.expect(requestTo(BASE + "/storage/v1/bucket"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.id").value("product-images"))
				.andExpect(jsonPath("$.public").value(true))
				.andRespond(withSuccess("{\"name\": \"product-images\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(OBJECT_URL)).andRespond(withSuccess());

		storage.store(KEY, CONTENT, "image/jpeg");

		server.verify();
	}

	@Test
	void uploadFailureBecomesStorageException() {
		server.expect(requestTo(OBJECT_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN)
				.contentType(MediaType.APPLICATION_JSON).body("{\"message\": \"Invalid API key\"}"));

		assertThatThrownBy(() -> storage.store(KEY, CONTENT, "image/jpeg"))
				.isInstanceOf(StorageException.class)
				.hasMessageContaining("Invalid API key");
	}

	@Test
	void deletesAndIgnoresMissingObject() {
		server.expect(requestTo(OBJECT_URL)).andExpect(method(HttpMethod.DELETE)).andRespond(withSuccess());
		server.expect(requestTo(OBJECT_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"statusCode\": \"404\", \"error\": \"not_found\", \"message\": \"Object not found\"}"));

		storage.delete(KEY);
		storage.delete(KEY);

		server.verify();
	}

	@Test
	void buildsPublicUrl() {
		assertThat(storage.publicUrl(KEY))
				.isEqualTo(BASE + "/storage/v1/object/public/product-images/" + KEY);
	}
}
