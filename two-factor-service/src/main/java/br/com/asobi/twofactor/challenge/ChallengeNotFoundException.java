package br.com.asobi.twofactor.challenge;

public class ChallengeNotFoundException extends RuntimeException {

	public ChallengeNotFoundException(String message) {
		super(message);
	}
}
