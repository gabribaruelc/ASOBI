package br.com.asobi.newsletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.newsletter.model.NewsletterCampaign;
import br.com.asobi.newsletter.model.NewsletterSubscriber;
import br.com.asobi.newsletter.repository.NewsletterCampaignRepository;
import br.com.asobi.newsletter.repository.NewsletterDeliveryRepository;
import br.com.asobi.newsletter.repository.NewsletterSubscriberRepository;
import br.com.asobi.newsletter.service.NewsletterService;
import br.com.asobi.notification.EmailMessage;
import br.com.asobi.notification.EmailSender;

@SpringBootTest(properties = "asobi.newsletter.daily-limit=3")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NewsletterCampaignTests {

	private static final String ADMIN = "admin@asobi.com.br";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NewsletterService newsletterService;

	@Autowired
	private NewsletterSubscriberRepository subscriberRepository;

	@Autowired
	private NewsletterCampaignRepository campaignRepository;

	@Autowired
	private NewsletterDeliveryRepository deliveryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private EmailSender emailSender;

	private Long createCampaign() throws Exception {
		mockMvc.perform(post("/admin/newsletter/campanhas").with(user(ADMIN)).with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("subject", "Jogos novos para as férias")
				.param("body", "Oi!\n\nChegaram jogos novos.")
				.param("buttonLabel", "")
				.param("buttonUrl", ""))
				.andExpect(status().is3xxRedirection());
		return campaignRepository.findAll().get(0).getId();
	}

	private void send(Long id) throws Exception {
		mockMvc.perform(post("/admin/newsletter/campanhas/{id}/envio", id).with(user(ADMIN)).with(csrf()))
				.andExpect(redirectedUrl("/admin/newsletter/campanhas/" + id));
	}

	@SuppressWarnings("unchecked")
	private List<EmailMessage> sentMessages() {
		ArgumentCaptor<List<EmailMessage>> captor = ArgumentCaptor.forClass(List.class);
		verify(emailSender, atLeastOnce()).sendAll(captor.capture());
		return captor.getAllValues().stream().flatMap(List::stream).toList();
	}

	/** Simula que os envios anteriores foram ontem: o limite diário libera de novo. */
	private void moveDeliveriesToYesterday() {
		jdbcTemplate.update("UPDATE newsletter_deliveries SET sent_at = sent_at - INTERVAL '25' HOUR");
	}

	@Test
	void sendsToEverySubscriberWithOwnUnsubscribeLink() throws Exception {
		newsletterService.subscribe("ana@example.com");
		// inscrição anterior à V15, sem token
		subscriberRepository.save(new NewsletterSubscriber("bia@example.com", null));
		Long id = createCampaign();

		send(id);

		List<EmailMessage> messages = sentMessages();
		assertThat(messages).extracting(message -> message.to().get(0))
				.containsExactly("ana@example.com", "bia@example.com");
		for (NewsletterSubscriber subscriber : subscriberRepository.findAll()) {
			assertThat(subscriber.getUnsubscribeToken()).isNotBlank();
			EmailMessage message = messages.stream().filter(m -> m.to().contains(subscriber.getEmail())).findFirst()
					.orElseThrow();
			assertThat(message.unsubscribeUrl()).endsWith("/newsletter/descadastro/" + subscriber.getUnsubscribeToken());
			assertThat(message.html()).contains("Chegaram jogos novos.", message.unsubscribeUrl());
		}
		assertThat(deliveryRepository.countByCampaignId(id)).isEqualTo(2);
	}

	@Test
	void cannotEditAfterSending() throws Exception {
		newsletterService.subscribe("ana@example.com");
		Long id = createCampaign();
		send(id);

		mockMvc.perform(post("/admin/newsletter/campanhas/{id}", id).with(user(ADMIN)).with(csrf())
				.param("subject", "Outro assunto").param("body", "Outro texto"))
				.andExpect(flash().attribute("error", containsString("não pode mais ser alterada")));

		assertThat(campaignRepository.findById(id)).get().extracting(NewsletterCampaign::getSubject)
				.isEqualTo("Jogos novos para as férias");
	}

	@Test
	void respectsDailyLimitAndContinuesWithoutRepeating() throws Exception {
		for (String name : List.of("ana", "bia", "caio", "duda")) {
			newsletterService.subscribe(name + "@example.com");
		}
		Long id = createCampaign();

		send(id);
		assertThat(deliveryRepository.countByCampaignId(id)).isEqualTo(3);

		// o limite do dia acabou
		send(id);
		assertThat(deliveryRepository.countByCampaignId(id)).isEqualTo(3);

		// quem se inscreveu depois do início não entra nesta newsletter
		newsletterService.subscribe("enzo@example.com");
		moveDeliveriesToYesterday();
		send(id);

		assertThat(sentMessages()).extracting(message -> message.to().get(0))
				.containsExactly("ana@example.com", "bia@example.com", "caio@example.com", "duda@example.com");
		mockMvc.perform(get("/admin/newsletter").with(user(ADMIN)))
				.andExpect(content().string(containsString("Enviada para 4")));
	}

	@Test
	void providerFailureReleasesTheBatch() throws Exception {
		newsletterService.subscribe("ana@example.com");
		Long id = createCampaign();
		willThrow(new IllegalStateException("Resend fora do ar")).given(emailSender).sendAll(any());

		mockMvc.perform(post("/admin/newsletter/campanhas/{id}/envio", id).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attribute("error", containsString("recusou o envio")));

		assertThat(deliveryRepository.countByCampaignId(id)).isZero();
	}

	@Test
	void testEmailGoesOnlyToTheAdmin() throws Exception {
		newsletterService.subscribe("ana@example.com");
		Long id = createCampaign();

		mockMvc.perform(post("/admin/newsletter/campanhas/{id}/teste", id).with(user(ADMIN)).with(csrf()))
				.andExpect(flash().attribute("success", "Teste enviado para " + ADMIN + "."));

		ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
		verify(emailSender).send(captor.capture());
		assertThat(captor.getValue().to()).containsExactly(ADMIN);
		assertThat(captor.getValue().subject()).isEqualTo("[Teste] Jogos novos para as férias");
		verify(emailSender, never()).sendAll(any());
		assertThat(deliveryRepository.count()).isZero();
	}

	@Test
	void showsPreviewOfDraft() throws Exception {
		Long id = createCampaign();

		mockMvc.perform(get("/admin/newsletter/campanhas/{id}", id).with(user(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(model().attribute("previewHtml", containsString("Chegaram jogos novos.")))
				.andExpect(content().string(containsString("Ver os jogos na loja")));
	}

	@Test
	void validatesForm() throws Exception {
		mockMvc.perform(post("/admin/newsletter/campanhas").with(user(ADMIN)).with(csrf())
				.param("subject", "").param("body", "Oi").param("buttonUrl", "javascript:alert(1)"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("form", "subject", "buttonUrl"));
		assertThat(campaignRepository.count()).isZero();
	}

	@Test
	void unsubscribeLinkAsksBeforeRemoving() throws Exception {
		newsletterService.subscribe("ana@example.com");
		String token = subscriberRepository.findAll().get(0).getUnsubscribeToken();

		mockMvc.perform(get("/newsletter/descadastro/{token}", token))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Quer sair da newsletter?")));
		assertThat(subscriberRepository.count()).isEqualTo(1);

		// sem CSRF: é o que o botão de descadastro do programa de e-mail manda
		mockMvc.perform(post("/newsletter/descadastro/{token}", token)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("List-Unsubscribe", "One-Click"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("você saiu da lista")));
		assertThat(subscriberRepository.count()).isZero();

		mockMvc.perform(get("/newsletter/descadastro/{token}", token))
				.andExpect(content().string(containsString("você saiu da lista")));
	}

	@Test
	void unsubscribingRemovesDeliveryHistory() throws Exception {
		newsletterService.subscribe("ana@example.com");
		Long id = createCampaign();
		send(id);
		String token = subscriberRepository.findAll().get(0).getUnsubscribeToken();

		mockMvc.perform(post("/newsletter/descadastro/{token}", token)).andExpect(status().isOk());
		subscriberRepository.flush();

		assertThat(deliveryRepository.count()).isZero();
	}

	@Test
	void campaignPagesRequireAdmin() throws Exception {
		mockMvc.perform(get("/admin/newsletter/campanhas/nova"))
				.andExpect(redirectedUrl("/admin/login"));
	}
}
