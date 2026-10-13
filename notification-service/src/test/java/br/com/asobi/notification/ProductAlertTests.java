package br.com.asobi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.notification.config.ApiKeyFilter;
import br.com.asobi.notification.email.EmailMessage;
import br.com.asobi.notification.email.EmailSender;
import br.com.asobi.notification.subscription.AlertType;
import br.com.asobi.notification.subscription.Subscription;
import br.com.asobi.notification.subscription.SubscriptionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductAlertTests {

	private static final String PROMOTION_EVENT = """
			{"type": "PROMOTION", "productId": 7, "productName": "Corrida dos Sapos <3",
			 "productUrl": "https://loja.asobi.test/jogos/corrida-dos-sapos", "price": 79.90, "originalPrice": 99.90}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@MockitoBean
	private EmailSender emailSender;

	private MockHttpServletRequestBuilder publish(String body) {
		return post("/api/product-events").header(ApiKeyFilter.HEADER, "test-key")
				.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private Subscription saved(String email, long productId, AlertType type) {
		return subscriptionRepository.save(new Subscription(email, productId, type,
				"token-" + email + "-" + productId + "-" + type, Instant.now()));
	}

	@SuppressWarnings("unchecked")
	private List<EmailMessage> sentMessages() {
		ArgumentCaptor<List<EmailMessage>> captor = ArgumentCaptor.forClass(List.class);
		verify(emailSender).sendAll(captor.capture());
		return captor.getValue();
	}

	@Test
	void notifiesOnlyWhoAskedForThatProductAndAlert() throws Exception {
		Subscription maria = saved("maria@example.com", 7, AlertType.PROMOTION);
		saved("joao@example.com", 7, AlertType.BACK_IN_STOCK);
		saved("ana@example.com", 8, AlertType.PROMOTION);

		mockMvc.perform(publish(PROMOTION_EVENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscribers").value(1))
				.andExpect(jsonPath("$.sent").value(1))
				.andExpect(jsonPath("$.failed").value(0))
				.andExpect(jsonPath("$.skipped").value(0));

		List<EmailMessage> messages = sentMessages();
		assertThat(messages).hasSize(1);
		EmailMessage message = messages.get(0);
		String unsubscribeUrl = "https://avisos.asobi.test/descadastro/" + maria.getUnsubscribeToken();
		assertThat(message.to()).isEqualTo("maria@example.com");
		assertThat(message.subject()).isEqualTo("Corrida dos Sapos <3 entrou em promoção — ASOBI");
		assertThat(message.unsubscribeUrl()).isEqualTo(unsubscribeUrl);
		assertThat(message.html())
				.contains("Corrida dos Sapos &lt;3")
				.contains("R$ 79,90")
				.contains("R$ 99,90")
				.contains("https://loja.asobi.test/jogos/corrida-dos-sapos")
				.contains(unsubscribeUrl);
		assertThat(subscriptionRepository.findById(maria.getId()).orElseThrow().getLastNotifiedAt()).isNotNull();
	}

	@Test
	void backInStockEmailHasNoCrossedOutPrice() throws Exception {
		saved("joao@example.com", 7, AlertType.BACK_IN_STOCK);

		mockMvc.perform(publish("""
				{"type": "BACK_IN_STOCK", "productId": 7, "productName": "Corrida dos Sapos",
				 "productUrl": "https://loja.asobi.test/jogos/corrida-dos-sapos", "price": 99.90}
				"""))
				.andExpect(jsonPath("$.sent").value(1));

		EmailMessage message = sentMessages().get(0);
		assertThat(message.subject()).isEqualTo("Corrida dos Sapos voltou ao estoque — ASOBI");
		assertThat(message.html()).contains("Voltou ao estoque!").contains("R$ 99,90").doesNotContain("line-through");
	}

	@Test
	void repeatedEventDoesNotEmailTheSamePersonAgain() throws Exception {
		saved("maria@example.com", 7, AlertType.PROMOTION);

		mockMvc.perform(publish(PROMOTION_EVENT)).andExpect(jsonPath("$.sent").value(1));
		mockMvc.perform(publish(PROMOTION_EVENT))
				.andExpect(jsonPath("$.subscribers").value(1))
				.andExpect(jsonPath("$.sent").value(0))
				.andExpect(jsonPath("$.skipped").value(1));

		verify(emailSender, times(1)).sendAll(any());
	}

	@Test
	void providerFailureIsReportedAndCanBeRetried() throws Exception {
		Subscription maria = saved("maria@example.com", 7, AlertType.PROMOTION);
		willThrow(new IllegalStateException("Resend fora do ar")).given(emailSender).sendAll(any());

		mockMvc.perform(publish(PROMOTION_EVENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sent").value(0))
				.andExpect(jsonPath("$.failed").value(1));

		// não foi marcada: o próximo evento tenta de novo
		assertThat(subscriptionRepository.findById(maria.getId()).orElseThrow().getLastNotifiedAt()).isNull();
	}

	@Test
	void eventWithoutSubscribersSendsNothing() throws Exception {
		mockMvc.perform(publish(PROMOTION_EVENT))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscribers").value(0))
				.andExpect(jsonPath("$.sent").value(0));

		verify(emailSender, never()).sendAll(any());
	}

	@Test
	void validatesEvent() throws Exception {
		mockMvc.perform(publish("""
				{"type": "PROMOTION", "productId": 7, "productName": " ", "productUrl": "javascript:alert(1)", "price": -1}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.productName").value("Informe o nome do produto."))
				.andExpect(jsonPath("$.errors.productUrl").value("Endereço do produto inválido."))
				.andExpect(jsonPath("$.errors.price").value("Preço inválido."));
	}

	@Test
	void unsubscribeLinkOnlyAsksOnGetAndCancelsOnPost() throws Exception {
		Subscription maria = saved("maria@example.com", 7, AlertType.PROMOTION);
		saved("maria@example.com", 8, AlertType.BACK_IN_STOCK);
		String link = "/descadastro/" + maria.getUnsubscribeToken();

		// sem X-Api-Key: é o navegador de quem recebeu o e-mail
		mockMvc.perform(get(link))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Quer parar de receber este aviso?")));
		assertThat(subscriptionRepository.count()).isEqualTo(2);

		mockMvc.perform(post(link))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("aviso cancelado")))
				.andExpect(content().string(Matchers.containsString("https://loja.asobi.test")));
		assertThat(subscriptionRepository.findById(maria.getId())).isEmpty();
		assertThat(subscriptionRepository.count()).isEqualTo(1);

		// abrir o link de novo não dá erro
		mockMvc.perform(get(link)).andExpect(content().string(Matchers.containsString("aviso cancelado")));
		mockMvc.perform(post(link)).andExpect(status().isOk());
	}

	@Test
	void unsubscribeCanCancelEveryAlertOfTheEmail() throws Exception {
		Subscription maria = saved("maria@example.com", 7, AlertType.PROMOTION);
		saved("maria@example.com", 8, AlertType.BACK_IN_STOCK);
		saved("joao@example.com", 7, AlertType.PROMOTION);

		mockMvc.perform(post("/descadastro/" + maria.getUnsubscribeToken()).param("all", "true"))
				.andExpect(status().isOk());

		assertThat(subscriptionRepository.findAll()).extracting(Subscription::getEmail)
				.containsExactly("joao@example.com");
	}
}
