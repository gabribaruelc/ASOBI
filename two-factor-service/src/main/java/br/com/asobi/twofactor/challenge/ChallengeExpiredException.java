package br.com.asobi.twofactor.challenge;

public class ChallengeExpiredException extends RuntimeException {

	public ChallengeExpiredException(String message) {
		super(message);
	}
}
