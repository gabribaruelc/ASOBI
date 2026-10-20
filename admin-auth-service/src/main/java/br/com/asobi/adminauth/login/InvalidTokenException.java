package br.com.asobi.adminauth.login;

public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
