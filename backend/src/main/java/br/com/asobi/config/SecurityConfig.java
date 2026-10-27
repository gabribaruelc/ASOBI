package br.com.asobi.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import br.com.asobi.admin.directory.AdminDirectory;
import br.com.asobi.admin.security.AdminOidcUserService;
import br.com.asobi.admin.security.CurrentAdmin;
import br.com.asobi.admin.twofactor.TwoFactorSession;

/**
 * Regras de acesso:
 * - /api/** é aberto para a loja e não usa sessão, então não precisa da proteção CSRF.
 * - /admin/** pergunta ao AdminDirectory, a cada clique, se o e-mail ainda é admin. Assim, quem
 *   for removido perde o acesso na hora, sem esperar o login vencer.
 * - Com o two-factor-service configurado, o painel só abre depois do código por e-mail; até lá
 *   o admin só consegue ver /admin/verificacao.
 * - O descadastro da newsletter é aberto e sem CSRF: quem prova que é a pessoa certa é o token
 *   do link, e o programa de e-mail faz o POST sozinho, sem formulário (padrão RFC 8058).
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			AdminDirectory adminDirectory,
			AdminOidcUserService adminOidcUserService,
			TwoFactorSession twoFactorSession,
			SecurityContextRepository securityContextRepository,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/admin/login", "/admin/dev-login").permitAll()
						.requestMatchers("/admin/verificacao", "/admin/verificacao/**")
						.access((authentication, context) -> new AuthorizationDecision(
								adminDirectory.isAdmin(CurrentAdmin.emailOf(authentication.get()))))
						.requestMatchers("/admin/**").access((authentication, context) -> {
							String email = CurrentAdmin.emailOf(authentication.get());
							return new AuthorizationDecision(adminDirectory.isAdmin(email)
									&& twoFactorSession.isSatisfied(context.getRequest(), email));
						})
						.anyRequest().permitAll())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**", "/newsletter/descadastro/**"))
				.cors(Customizer.withDefaults())
				// console do H2 usa frames
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/admin/login"))
						.accessDeniedHandler((request, response, denied) -> {
							String email = CurrentAdmin.emailOf(
									SecurityContextHolder.getContext().getAuthentication());
							if (adminDirectory.isAdmin(email) && !twoFactorSession.isSatisfied(request, email)) {
								// é admin, só falta o código do e-mail
								response.sendRedirect(request.getContextPath() + "/admin/verificacao");
								return;
							}
							// logado, mas o e-mail saiu da lista de admins
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
