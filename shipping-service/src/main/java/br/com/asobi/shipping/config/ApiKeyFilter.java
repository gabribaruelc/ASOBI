package br.com.asobi.shipping.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Só o backend da loja chama este serviço: /api/** exige o cabeçalho X-Api-Key.
 * O health check (/actuator/health) fica aberto para o Cloud Run.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Api-Key";

	private final byte[] expectedKey;

	public ApiKeyFilter(ApiKeyProperties properties) {
		this.expectedKey = StringUtils.hasText(properties.apiKey())
				? properties.apiKey().getBytes(StandardCharsets.UTF_8)
				: null;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return expectedKey == null || !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String given = request.getHeader(HEADER);
		if (given != null && MessageDigest.isEqual(expectedKey, given.getBytes(StandardCharsets.UTF_8))) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Chave de API inválida.\"}");
	}
}
