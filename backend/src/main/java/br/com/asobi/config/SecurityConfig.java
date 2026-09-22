package br.com.asobi.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import br.com.asobi.admin.security.AdminOidcUserService;
import br.com.asobi.admin.security.CurrentAdmin;
import br.com.asobi.admin.service.AdminUserService;

/**
 * - /api/**: loja (Next.js). Pública e sem sessão; CSRF desligado (não usa cookie de login).
 * - /admin/**: painel Thymeleaf. Exige login com Google E e-mail presente em admin_users,
 *   conferido no banco a cada requisição — remover um admin corta o acesso na hora.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			AdminUserService adminUserService,
			AdminOidcUserService adminOidcUserService,
			SecurityContextRepository securityContextRepository,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/admin/login", "/admin/dev-login").permitAll()
						.requestMatchers("/admin/**").access((authentication, context) ->
								new AuthorizationDecision(adminUserService.isAdmin(
										CurrentAdmin.emailOf(authentication.get()))))
						.anyRequest().permitAll())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
				.cors(Customizer.withDefaults())
				// Console do H2 (só no perfil local) usa frames.
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/admin/login"))
						.accessDeniedHandler((request, response, denied) -> {
							// Logado, mas sem permissão (ex.: e-mail removido da lista de admins).
							var session = request.getSession(false);
							if (session != null) {
								session.invalidate();
							}
							response.sendRedirect(request.getContextPath() + "/admin/login?denied");
						}))
				.logout(logout -> logout
						.logoutUrl("/admin/logout")
						.logoutSuccessUrl("/admin/login?logout"));

		if (clientRegistrations.getIfAvailable() != null) {
			http.oauth2Login(oauth -> oauth
					.loginPage("/admin/login")
					.userInfoEndpoint(userInfo -> userInfo.oidcUserService(adminOidcUserService))
					.defaultSuccessUrl("/admin", true)
					.failureUrl("/admin/login?error"));
		}

		return http.build();
	}
}
