package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.util.List;

/** Cotação de frete (hoje: microsserviço shipping-service, que fala com o Melhor Envio). */
public interface ShippingQuoteProvider {

	/** Tem credenciais para cotar? Sem isso o frete automático não pode ser ligado. */
	boolean isConfigured();

	/** Opções disponíveis, da mais barata para a mais cara (sem frete grátis aplicado). */
	List<ShippingOption> quote(QuoteRequest request);

	record QuoteRequest(String originPostalCode, String destinationPostalCode, List<Parcel> parcels) {
	}

	/** Um produto do carrinho, com a caixa padrão configurada no painel. */
	record Parcel(String id, int quantity, BigDecimal unitValue, BigDecimal weightKg, int widthCm, int heightCm,
			int lengthCm) {
	}
}
