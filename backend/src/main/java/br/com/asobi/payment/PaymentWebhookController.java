package br.com.asobi.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.order.service.OrderService;

/**
 * Notificações do Mercado Pago. Nunca confia no corpo da notificação: usa só o
 * id do pagamento e consulta o pagamento na API do Mercado Pago.
 */
@RestController
public class PaymentWebhookController {

	private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

	private final PaymentGateway paymentGateway;
	private final OrderService orderService;
	private final MercadoPagoSignatureVerifier signatureVerifier;

	public PaymentWebhookController(PaymentGateway paymentGateway, OrderService orderService,
			MercadoPagoProperties mercadoPago) {
		this.paymentGateway = paymentGateway;
		this.orderService = orderService;
		this.signatureVerifier = StringUtils.hasText(mercadoPago.webhookSecret())
				? new MercadoPagoSignatureVerifier(mercadoPago.webhookSecret())
				: null;
		if (signatureVerifier == null) {
			log.warn("MERCADO_PAGO_WEBHOOK_SECRET não definido: notificações aceitas sem conferir assinatura.");
		}
	}

	@PostMapping("/api/webhooks/mercadopago")
	public ResponseEntity<Void> receive(
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "data.id", required = false) String dataId,
			@RequestHeader(name = "x-signature", required = false) String signature,
			@RequestHeader(name = "x-request-id", required = false) String requestId) {

		if (signatureVerifier != null && !signatureVerifier.isValid(signature, requestId, dataId)) {
			log.warn("Notificação do Mercado Pago com assinatura inválida (data.id={})", dataId);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		// Só interessam pagamentos (ignora merchant_order, planos etc.).
		if (!"payment".equals(type) || !StringUtils.hasText(dataId)) {
			return ResponseEntity.ok().build();
		}

		paymentGateway.fetchPayment(dataId).ifPresentOrElse(update -> {
			try {
				orderService.applyPayment(update);
			} catch (NotFoundException ex) {
				// Pagamento de outro sistema/conta: responde 200 para o Mercado Pago não reenviar.
				log.warn("Pagamento {} aponta para pedido inexistente {}", dataId, update.orderPublicId());
			}
		}, () -> log.warn("Pagamento {} não encontrado no Mercado Pago", dataId));
		return ResponseEntity.ok().build();
	}
}
