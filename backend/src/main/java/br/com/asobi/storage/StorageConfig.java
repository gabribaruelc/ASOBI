package br.com.asobi.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Com SUPABASE_URL e SUPABASE_SERVICE_KEY definidos, as fotos vão para o Supabase
 * Storage; sem eles, para uma pasta local (desenvolvimento).
 */
@Configuration
public class StorageConfig {

	private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

	@Bean
	public ImageStorage imageStorage(StorageProperties properties,
			@Value("${asobi.public-url}") String publicUrl) {
		if (StringUtils.hasText(properties.supabaseUrl()) && StringUtils.hasText(properties.supabaseKey())) {
			log.info("Fotos: Supabase Storage (bucket \"{}\")", properties.bucket());
			return new SupabaseImageStorage(properties);
		}
		log.warn("Fotos: pasta local {} (SUPABASE_URL/SUPABASE_SERVICE_KEY não definidos).", properties.localDir());
		return new LocalImageStorage(properties.localDir(), publicUrl);
	}

	/** Serve a pasta local em /uploads/** (só existe no modo local). */
	@Bean
	public WebMvcConfigurer localUploadsConfigurer(ImageStorage imageStorage) {
		return new WebMvcConfigurer() {
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				if (imageStorage instanceof LocalImageStorage local) {
					String location = local.baseDir().toUri().toString();
					registry.addResourceHandler(LocalImageStorage.URL_PATH + "**")
							.addResourceLocations(location.endsWith("/") ? location : location + "/");
				}
			}
		};
	}
}
