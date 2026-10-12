package br.com.asobi.adminauth.login;

public class LoginUnavailableException extends RuntimeException {

	public LoginUnavailableException(String message) {
		super(message);
	}
}
