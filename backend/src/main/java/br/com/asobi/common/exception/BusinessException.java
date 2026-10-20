package br.com.asobi.common.exception;

/** HTTP 422 na API. */
public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}
}
