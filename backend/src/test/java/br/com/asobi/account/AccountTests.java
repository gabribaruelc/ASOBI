package br.com.asobi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.com.asobi.order.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountTests {

	private static final UUID MARIA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ANA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderRepository orderRepository;

	@MockitoBean
	private SupabaseAuthClient supabaseAuthClient;

	@BeforeEach
	void knownTokens() {
		when(supabaseAuthClient.verify("maria-token"))
				.thenReturn(Optional.of(new CustomerIdentity(MARIA_ID, "maria@example.com", true)));
		when(supabaseAuthClient.verify("ana-token"))
				.thenReturn(Optional.of(new CustomerIdentity(ANA_ID, "ana@example.com", true)));
		when(supabaseAuthClient.verify("ana-unverified-token"))
				.thenReturn(Optional.of(new CustomerIdentity(ANA_ID, "maria@example.com", false)));
	}

	/** @return id público do pedido criado */
	private String placeOrder(String email, String token) throws Exception {
		MockHttpServletRequestBuilder request = post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"customer": {"name": "Cliente Teste", "email": "%s", "phone": "(11) 98765-4321"},
				 "shippingAddress": {"postalCode": "01310100", "street": "Av. Paulista", "number": "1000",
				                     "district": "Bela Vista", "city": "São Paulo", "state": "SP"},
				 "items": [{"slug": "alvo-certeiro", "quantity": 1}]}
				""".formatted(email));
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		String body = mockMvc.perform(request).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.orderId");
	}

	@Test
	void requiresLogin() throws Exception {
		mockMvc.perform(get("/api/account/orders"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Entre na sua conta para continuar."));
		mockMvc.perform(get("/api/account/orders").header("Authorization", "Bearer token-vencido"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listsOrdersPlacedWhileLoggedIn() throws Exception {
		// Logada, mas com outro e-mail de contato no checkout: vale a conta.
		String own = placeOrder("contato-da-maria@example.com", "maria-token");
		placeOrder("outra-pessoa@example.com", null);

		assertThat(orderRepository.findByPublicId(UUID.fromString(own)).orElseThrow().getCustomerUserId())
				.isEqualTo(MARIA_ID);
		mockMvc.perform(get("/api/account/orders").header("Authorization", "Bearer maria-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].orderId").value(own))
				.andExpect(jsonPath("$[0].statusLabel").value("Aguardando pagamento"))
				.andExpect(jsonPath("$[0].items[0].name").value("Alvo Certeiro"))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Paulista"))));
	}

	@Test
	void includesGuestOrdersWithTheSameVerifiedEmail() throws Exception {
		String guest = placeOrder("Maria@Example.com", null);

		mockMvc.perform(get("/api/account/orders").header("Authorization", "Bearer maria-token"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].orderId").value(guest));
		mockMvc.perform(get("/api/account/orders").header("Authorization", "Bearer ana-token"))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unverifiedEmailDoesNotClaimGuestOrders() throws Exception {
		placeOrder("maria@example.com", null);

		mockMvc.perform(get("/api/account/orders").header("Authorization", "Bearer ana-unverified-token"))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void expiredLoginStillPlacesGuestOrder() throws Exception {
		String id = placeOrder("maria@example.com", "token-vencido");

		assertThat(orderRepository.findByPublicId(UUID.fromString(id)).orElseThrow().getCustomerUserId()).isNull();
	}
}
