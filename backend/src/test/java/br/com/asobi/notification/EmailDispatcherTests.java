package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EmailDispatcherTests {

	private static final EmailMessage MESSAGE = new EmailMessage(List.of("maria@example.com"), "Assunto", "<p>Oi</p>");

	private final EmailSender sender = mock(EmailSender.class);
	private final EmailDispatcher dispatcher = new EmailDispatcher(sender);

	@AfterEach
	void clearTransaction() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void sendsRightAwayOutsideTransaction() {
		dispatcher.sendAfterCommit(MESSAGE);
		verify(sender).send(MESSAGE);
	}

	@Test
	void insideTransactionWaitsForCommit() {
		TransactionSynchronizationManager.initSynchronization();

		dispatcher.sendAfterCommit(MESSAGE);
		verify(sender, never()).send(MESSAGE);

		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
		verify(sender).send(MESSAGE);
	}

	@Test
	void rolledBackTransactionSendsNothing() {
		TransactionSynchronizationManager.initSynchronization();
		dispatcher.sendAfterCommit(MESSAGE);

		TransactionSynchronizationManager.getSynchronizations()
				.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
		verify(sender, never()).send(MESSAGE);
	}

	@Test
	void providerFailureDoesNotPropagate() {
		doThrow(new IllegalStateException("Resend fora do ar")).when(sender).send(MESSAGE);
		assertThatCode(() -> dispatcher.sendAfterCommit(MESSAGE)).doesNotThrowAnyException();
	}
}
