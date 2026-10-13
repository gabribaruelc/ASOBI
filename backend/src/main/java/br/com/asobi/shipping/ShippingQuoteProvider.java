package br.com.asobi.shipping;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingQuoteProvider {

	boolean isConfigured();

	/** @return da mais barata para a mais cara, sem frete grátis aplicado */
	List<ShippingOption> quote(QuoteRequest request);

	record QuoteRequest(String originPostalCode, String destinationPostalCode, List<Parcel> parcels) {
	}

	record Parcel(String id, int quantity, BigDecimal unitValue, BigDecimal weightKg, int widthCm, int heightCm,
			int lengthCm) {
	}
}
