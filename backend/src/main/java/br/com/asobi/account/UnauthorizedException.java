package br.com.asobi.account;

/** A rota exige cliente logado e o login não veio ou não vale mais (HTTP 401). */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
