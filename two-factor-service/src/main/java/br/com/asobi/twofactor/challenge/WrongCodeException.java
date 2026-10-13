package br.com.asobi.twofactor.challenge;

public class WrongCodeException extends RuntimeException {

	private final int attemptsLeft;

	public WrongCodeException(String message, int attemptsLeft) {
		super(message);
		this.attemptsLeft = attemptsLeft;
	}

	public int getAttemptsLeft() {
		return attemptsLeft;
	}
}
