package br.com.asobi.account;

import java.util.Optional;

import org.springframework.stereotype.Component;

/** Lê o cabeçalho "Authorization: Bearer ..." enviado pela loja e descobre quem é o cliente. */
@Component
public class CustomerAuth {

	private static final String BEARER = "Bearer ";

	private final SupabaseAuthClient supabaseAuthClient;

	public CustomerAuth(SupabaseAuthClient supabaseAuthClient) {
		this.supabaseAuthClient = supabaseAuthClient;
	}

	/** Para rotas em que o login é opcional (ex.: fechar pedido). */
	public Optional<CustomerIdentity> optional(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
			return Optional.empty();
		}
		return supabaseAuthClient.verify(authorizationHeader.substring(BEARER.length()).trim());
	}

	/** Para rotas que só existem para quem está logado. */
	public CustomerIdentity require(String authorizationHeader) {
		return optional(authorizationHeader)
				.orElseThrow(() -> new UnauthorizedException("Entre na sua conta para continuar."));
	}
}
