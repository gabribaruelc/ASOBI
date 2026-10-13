package br.com.asobi.adminauth.login;

public class NotAdminException extends RuntimeException {

	public NotAdminException(String message) {
		super(message);
	}
}
