package br.com.asobi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera a loja (Next.js, em outro domínio) para consumir a API REST.
 * O painel admin é servido pelo próprio Spring (Thymeleaf) e não precisa de CORS.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] frontendOrigins;

	public CorsConfig(@Value("${asobi.frontend-origins}") String[] frontendOrigins) {
		this.frontendOrigins = frontendOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(frontendOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
