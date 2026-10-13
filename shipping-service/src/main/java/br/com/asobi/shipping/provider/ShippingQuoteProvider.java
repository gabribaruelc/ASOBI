package br.com.asobi.shipping.provider;

import java.util.List;

import br.com.asobi.shipping.quote.QuoteRequest;
import br.com.asobi.shipping.quote.ShippingOption;

public interface ShippingQuoteProvider {

	String name();

	boolean isConfigured();

	/**
	 * @return da mais barata para a mais cara
	 * @throws ProviderUnavailableException sem credenciais configuradas
	 * @throws ProviderFailureException     a transportadora falhou ou não respondeu
	 */
	List<ShippingOption> quote(QuoteRequest request);
}
