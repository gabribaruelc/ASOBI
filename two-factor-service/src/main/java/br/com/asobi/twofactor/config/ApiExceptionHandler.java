package br.com.asobi.twofactor.config;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.asobi.twofactor.challenge.ChallengeExpiredException;
import br.com.asobi.twofactor.challenge.ChallengeNotFoundException;
import br.com.asobi.twofactor.challenge.EmailDeliveryException;
import br.com.asobi.twofactor.challenge.TooManyChallengesException;
import br.com.asobi.twofactor.challenge.WrongCodeException;

/** RFC 9457; o detail já vem pronto para mostrar a quem está digitando o código. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ChallengeNotFoundException.class)
	public ProblemDetail handleNotFound(ChallengeNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ChallengeExpiredException.class)
	public ProblemDetail handleExpired(ChallengeExpiredException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
	}

	@ExceptionHandler(WrongCodeException.class)
	public ProblemDetail handleWrongCode(WrongCodeException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problem.setProperty("attemptsLeft", ex.getAttemptsLeft());
		return problem;
	}

	@ExceptionHandler(TooManyChallengesException.class)
	public ProblemDetail handleTooMany(TooManyChallengesException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
	}

	@ExceptionHandler(EmailDeliveryException.class)
	public ProblemDetail handleEmailDelivery(EmailDeliveryException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField,
						error -> String.valueOf(error.getDefaultMessage()),
						(first, second) -> first));
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}
}
