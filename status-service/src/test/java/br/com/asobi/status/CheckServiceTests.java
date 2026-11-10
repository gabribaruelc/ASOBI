package br.com.asobi.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.status.check.CheckService;
import br.com.asobi.status.check.HealthProber;
import br.com.asobi.status.check.HealthProber.Probe;
import br.com.asobi.status.email.EmailMessage;
import br.com.asobi.status.email.EmailSender;
import br.com.asobi.status.recipient.AdminListClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CheckServiceTests {

	private static final String STORE = "https://asobi.com.br";
	private static final String BACKEND = "https://backend.test/actuator/health";

	@Autowired
	private CheckService checkService;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HealthProber prober;

	@MockitoBean
	private AdminListClient adminList;

	@MockitoBean
	private EmailSender emailSender;

	@MockitoBean
	private Clock clock;

	private Instant now = Instant.parse("2026-11-02T15:00:00Z");

	@BeforeEach
	void setUp() {
		given(clock.instant()).willAnswer(invocation -> now);
		given(adminList.isConfigured()).willReturn(true);
		given(adminList.listAdminEmails()).willReturn(List.of("b@asobi.com.br", "A@asobi.com.br"));
		given(prober.probe(eq(STORE), any())).willReturn(Probe.ok());
		backendIs(Probe.ok());
	}

	private void backendIs(Probe probe) {
		given(prober.probe(eq(BACKEND), any())).willReturn(probe);
	}

	private void round(Duration later) {
		now = now.plus(later);
		checkService.run();
	}

	private EmailMessage lastEmail() {
		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(emailSender).send(captor.capture());
		return captor.getValue();
	}

	@Test
	void everythingUpSendsNothing() {
		round(Duration.ZERO);
		verify(emailSender, never()).send(any());
	}

	@Test
	void secondFailureInARowWarnsEveryAdminOnce() {
		backendIs(Probe.failed("não respondeu em 30 segundos"));
		round(Duration.ZERO);
		verify(emailSender, never()).send(any());

		round(Duration.ofMinutes(5));
		EmailMessage email = lastEmail();
		assertThat(email.to()).containsExactly("a@asobi.com.br", "b@asobi.com.br");
		assertThat(email.subject()).isEqualTo("⚠️ ASOBI: parou de funcionar — API da loja e painel admin");
		assertThat(email.text()).contains("não respondeu em 30 segundos", "desde 02/11 às 12:00 (5 min)",
				"O que fazer: Sem ele a loja não mostra produtos");

		clearInvocations(emailSender);
		round(Duration.ofMinutes(5));
		verify(emailSender, never()).send(any());
	}

	@Test
	void recoveryIsAnnouncedOnlyAfterAnAlert() {
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ZERO);
		backendIs(Probe.ok());
		round(Duration.ofMinutes(5));
		verify(emailSender, never()).send(any());

		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ofMinutes(5));
		round(Duration.ofMinutes(5));
		clearInvocations(emailSender);
		backendIs(Probe.ok());
		round(Duration.ofMinutes(10));

		EmailMessage email = lastEmail();
		assertThat(email.subject()).isEqualTo("✅ ASOBI: voltou a funcionar — API da loja e painel admin");
		assertThat(email.text()).contains("voltou depois de 15 min");

		clearInvocations(emailSender);
		round(Duration.ofMinutes(5));
		verify(emailSender, never()).send(any());
	}

	@Test
	void stillDownIsRemindedAfterSixHours() {
		backendIs(Probe.failed("respondeu com erro (HTTP 503)"));
		round(Duration.ZERO);
		round(Duration.ofMinutes(5));
		clearInvocations(emailSender);

		round(Duration.ofHours(5));
		verify(emailSender, never()).send(any());
		round(Duration.ofHours(1));
		assertThat(lastEmail().subject()).isEqualTo("⚠️ ASOBI: continua parado — API da loja e painel admin");
	}

	@Test
	void failedSendIsTriedAgainNextRound() {
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ZERO);
		willThrow(new IllegalStateException("Resend fora do ar")).given(emailSender).send(any());
		round(Duration.ofMinutes(5));

		reset(emailSender);
		round(Duration.ofMinutes(5));
		verify(emailSender, times(1)).send(any());
	}

	@Test
	void usesTheLastKnownAdminsWhenTheListIsUnavailable() {
		round(Duration.ZERO);
		given(adminList.listAdminEmails()).willThrow(new IllegalStateException("admin-auth-service fora do ar"));
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ofMinutes(5));
		round(Duration.ofMinutes(5));

		assertThat(lastEmail().to()).containsExactly("a@asobi.com.br", "b@asobi.com.br");
	}

	@Test
	void removedAdminStopsGettingWarnings() {
		round(Duration.ZERO);
		given(adminList.listAdminEmails()).willReturn(List.of("b@asobi.com.br"));
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ofMinutes(5));
		round(Duration.ofMinutes(5));

		assertThat(lastEmail().to()).containsExactly("b@asobi.com.br");
	}

	@Test
	void fallsBackWhenNoAdminWasEverRead() {
		given(adminList.listAdminEmails()).willThrow(new IllegalStateException("admin-auth-service fora do ar"));
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ZERO);
		round(Duration.ofMinutes(5));

		assertThat(lastEmail().to()).containsExactly("reserva@asobi.com.br");
	}

	@Test
	void namesTheBrokenPartAndWhatToDo() {
		backendIs(Probe.ok(Map.of("db", "UP", "resend", "DOWN", "diskSpace", "UP")));
		round(Duration.ZERO);
		round(Duration.ofMinutes(5));

		EmailMessage email = lastEmail();
		assertThat(email.subject()).isEqualTo("⚠️ ASOBI: parou de funcionar — E-mails da loja (Resend)");
		assertThat(email.text())
				.contains("parou de funcionar (afeta: API da loja e painel admin)", "O que fazer: Clientes não estão")
				.doesNotContain("Banco de dados");

		clearInvocations(emailSender);
		backendIs(Probe.ok(Map.of("db", "UP", "resend", "UP")));
		round(Duration.ofMinutes(5));
		assertThat(lastEmail().subject()).isEqualTo("✅ ASOBI: voltou a funcionar — E-mails da loja (Resend)");
	}

	@Test
	void unreachableServiceKeepsItsPartsAsTheyWere() {
		backendIs(Probe.ok(Map.of("resend", "DOWN")));
		round(Duration.ZERO);
		backendIs(Probe.failed("não foi possível conectar"));
		round(Duration.ofMinutes(5));
		backendIs(Probe.ok(Map.of("resend", "DOWN")));
		round(Duration.ofMinutes(5));

		assertThat(lastEmail().subject()).isEqualTo("⚠️ ASOBI: parou de funcionar — E-mails da loja (Resend)");
	}

	@Test
	void sameBrokenPartInTwoServicesIsOneItem() {
		given(prober.probe(eq(STORE), any())).willReturn(Probe.ok(Map.of("db", "DOWN")));
		backendIs(Probe.ok(Map.of("db", "DOWN")));
		round(Duration.ZERO);
		round(Duration.ofMinutes(5));

		EmailMessage email = lastEmail();
		assertThat(email.subject()).isEqualTo("⚠️ ASOBI: parou de funcionar — Banco de dados (Supabase)");
		assertThat(email.text()).contains("(afeta: Loja (site), API da loja e painel admin)");
	}

	@Test
	void checksEndpointNeedsTheKeyAndSkipsServicesNotDeployed() throws Exception {
		mockMvc.perform(post("/api/checks")).andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/checks").header("X-Api-Key", "test-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].name").value("backend"))
				.andExpect(jsonPath("$[1].up").value(true));
	}
}
