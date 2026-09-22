package br.com.asobi.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderItem;

/**
 * Mercado Pago Checkout Pro: o cliente paga (Pix, cartão, boleto) na página do
 * Mercado Pago. Nenhum dado de cartão passa pela ASOBI.
 */
public class MercadoPagoGateway implements PaymentGateway {

	private static final Logger log = LoggerFactory.getLogger(MercadoPagoGateway.class);

	private final String accessToken;
	private final String storeUrl;
	private final String notificationUrl;
	private final PreferenceClient preferenceClient;
	private final PaymentClient paymentClient;

	/**
	 * @param storeUrl  loja (Next.js), para onde o cliente volta depois de pagar
	 * @param publicUrl endereço público deste backend, para o webhook
	 */
	public MercadoPagoGateway(String accessToken, String storeUrl, String publicUrl) {
		this(accessToken, storeUrl, publicUrl, new PreferenceClient(), new PaymentClient());
	}

	MercadoPagoGateway(String accessToken, String storeUrl, String publicUrl, PreferenceClient preferenceClient,
			PaymentClient paymentClient) {
		this.accessToken = accessToken;
		this.storeUrl = storeUrl.replaceAll("/$", "");
		this.notificationUrl = publicUrl.replaceAll("/$", "") + "/api/webhooks/mercadopago";
		this.preferenceClient = preferenceClient;
		this.paymentClient = paymentClient;
		MercadoPagoConfig.setAccessToken(accessToken);
	}

	@Override
	public String name() {
		return "mercadopago";
	}

	@Override
	public String createCheckout(Order order) {
		try {
			Preference preference = preferenceClient.create(buildPreferenceRequest(order));
			// Credenciais de teste (TEST-...) usam o checkout de sandbox.
			return accessToken.startsWith("TEST-") ? preference.getSandboxInitPoint() : preference.getInitPoint();
		} catch (MPApiException ex) {
			log.error("Mercado Pago recusou a preferência do pedido #{}: {} {}", order.getId(),
					ex.getStatusCode(), ex.getApiResponse() == null ? "" : ex.getApiResponse().getContent());
			throw new PaymentException("Não foi possível iniciar o pagamento. Tente novamente em instantes.", ex);
		} catch (MPException ex) {
			log.error("Falha ao falar com o Mercado Pago (pedido #{})", order.getId(), ex);
			throw new PaymentException("Não foi possível iniciar o pagamento. Tente novamente em instantes.", ex);
		}
	}

	@Override
	public Optional<PaymentUpdate> fetchPayment(String paymentId) {
		long id;
		try {
			id = Long.parseLong(paymentId);
		} catch (NumberFormatException ex) {
			return Optional.empty();
		}
		try {
			Payment payment = paymentClient.get(id);
			UUID orderId = parseUuid(payment.getExternalReference());
			if (orderId == null) {
				log.warn("Pagamento {} sem external_reference de pedido ASOBI", paymentId);
				return Optional.empty();
			}
			return Optional.of(new PaymentUpdate(String.valueOf(payment.getId()), orderId, payment.getStatus(),
					payment.getPaymentMethodId(), outcomeOf(payment.getStatus())));
		} catch (MPApiException ex) {
			if (ex.getStatusCode() == 404) {
				return Optional.empty();
			}
			throw new PaymentException("Falha ao consultar o pagamento " + paymentId, ex);
		} catch (MPException ex) {
			throw new PaymentException("Falha ao consultar o pagamento " + paymentId, ex);
		}
	}

	PreferenceRequest buildPreferenceRequest(Order order) {
		List<PreferenceItemRequest> items = new ArrayList<>();
		for (OrderItem item : order.getItems()) {
			items.add(PreferenceItemRequest.builder()
					.id(item.getProductSlug())
					.title(item.getProductName())
					.quantity(item.getQuantity())
					.unitPrice(item.getUnitPrice())
					.currencyId("BRL")
					.build());
		}
		if (order.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
			items.add(PreferenceItemRequest.builder()
					.id("frete")
					.title("Frete")
					.quantity(1)
					.unitPrice(order.getShippingCost())
					.currencyId("BRL")
					.build());
		}

		String orderPage = storeUrl + "/pedido/" + order.getPublicId();
		PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
				.items(items)
				.externalReference(order.getPublicId().toString())
				.payer(PreferencePayerRequest.builder()
						.name(order.getCustomer().getName())
						.email(order.getCustomer().getEmail())
						.build())
				.backUrls(PreferenceBackUrlsRequest.builder()
						.success(orderPage)
						.pending(orderPage)
						.failure(orderPage)
						.build())
				.statementDescriptor("ASOBI");

		// O Mercado Pago só aceita retorno automático e webhook para endereços públicos em HTTPS.
		if (storeUrl.startsWith("https://")) {
			builder.autoReturn("approved");
		}
		if (notificationUrl.startsWith("https://")) {
			builder.notificationUrl(notificationUrl);
		}
		return builder.build();
	}

	static PaymentUpdate.Outcome outcomeOf(String status) {
		if (status == null) {
			return PaymentUpdate.Outcome.PENDING;
		}
		return switch (status) {
			case "approved" -> PaymentUpdate.Outcome.APPROVED;
			case "rejected", "cancelled", "refunded", "charged_back" -> PaymentUpdate.Outcome.FAILED;
			default -> PaymentUpdate.Outcome.PENDING; // pending, in_process, authorized, in_mediation
		};
	}

	private static UUID parseUuid(String value) {
		try {
			return value == null ? null : UUID.fromString(value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
