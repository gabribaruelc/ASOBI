package br.com.asobi.common.exception;

/** Regra de negócio violada (ex.: estoque insuficiente). Vira HTTP 422 na API. */
public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}
}
