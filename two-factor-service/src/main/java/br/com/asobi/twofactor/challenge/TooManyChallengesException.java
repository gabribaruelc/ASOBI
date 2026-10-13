package br.com.asobi.twofactor.challenge;

public class TooManyChallengesException extends RuntimeException {

	public TooManyChallengesException(String message) {
		super(message);
	}
}
