package br.com.asobi.shipping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.asobi.shipping.config.ApiKeyFilter;
import br.com.asobi.shipping.provider.ProviderFailureException;
import br.com.asobi.shipping.provider.ProviderUnavailableException;
import br.com.asobi.shipping.provider.ShippingQuoteProvider;
import br.com.asobi.shipping.quote.ShippingOption;

@SpringBootTest(properties = "shipping-service.api-key=test-key")
@AutoConfigureMockMvc
class QuoteApiTests {

	private static final String BODY = """
			{"originPostalCode": "01310-100", "destinationPostalCode": "20040002",
			 "parcels": [{"id": "alvo-certeiro", "quantity": 2, "unitValue": 89.90, "weightKg": 1.0,
			              "widthCm": 30, "heightCm": 8, "lengthCm": 30}]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShippingQuoteProvider provider;

	private MockHttpServletRequestBuilder quote(String body) {
		return post("/api/quotes").header(ApiKeyFilter.HEADER, "test-key")
				.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void returnsQuotedOptions() throws Exception {
		given(provider.quote(any())).willReturn(List.of(
				new ShippingOption("1", "PAC", "Correios", new BigDecimal("25.50"), 6)));
		mockMvc.perform(quote(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("1"))
				.andExpect(jsonPath("$[0].price").value(25.50))
				.andExpect(jsonPath("$[0].deliveryDays").value(6));
	}

	@Test
	void rejectsRequestsWithoutValidApiKey() throws Exception {
		mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/provider").header(ApiKeyFilter.HEADER, "wrong"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthCheckIsOpen() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void validatesPostalCodesAndParcels() throws Exception {
		mockMvc.perform(quote("""
				{"originPostalCode": "123", "destinationPostalCode": "20040-002", "parcels": []}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.originPostalCode").value("CEP de origem inválido."))
				.andExpect(jsonPath("$.errors.parcels").value("Informe ao menos um volume."));
	}

	@Test
	void reportsProviderStatus() throws Exception {
		given(provider.name()).willReturn("melhor-envio");
		given(provider.isConfigured()).willReturn(true);
		mockMvc.perform(get("/api/provider").header(ApiKeyFilter.HEADER, "test-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("melhor-envio"))
				.andExpect(jsonPath("$.configured").value(true));
	}

	@Test
	void mapsProviderErrorsToProblemDetails() throws Exception {
		given(provider.quote(any()))
				.willThrow(new ProviderUnavailableException("Cálculo de frete indisponível no momento."));
		mockMvc.perform(quote(BODY))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.detail").value("Cálculo de frete indisponível no momento."));

		willThrow(new ProviderFailureException("Falhou.", null)).given(provider).quote(any());
		mockMvc.perform(quote(BODY)).andExpect(status().isBadGateway());
	}
}
