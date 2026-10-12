package br.com.asobi.order.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.account.CustomerAuth;
import br.com.asobi.order.dto.OrderCreatedResponse;
import br.com.asobi.order.dto.OrderRequest;
import br.com.asobi.order.dto.OrderStatusResponse;
import br.com.asobi.order.service.OrderService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;
	private final CustomerAuth customerAuth;

	public OrderController(OrderService orderService, CustomerAuth customerAuth) {
		this.orderService = orderService;
		this.customerAuth = customerAuth;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderCreatedResponse placeOrder(@Validated @RequestBody OrderRequest request,
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		// Login é opcional: com ele, o pedido fica na conta; sem ele (ou vencido), segue sem cadastro.
		return orderService.placeOrder(request, customerAuth.optional(authorization));
	}

	@GetMapping("/{orderId}")
	public OrderStatusResponse getOrder(@PathVariable UUID orderId) {
		return orderService.getOrderStatus(orderId);
	}

	/** Chamado pela página do pedido quando o cliente volta do Mercado Pago (?payment_id=...). */
	@PostMapping("/{orderId}/payment-sync")
	public OrderStatusResponse syncPayment(@PathVariable UUID orderId,
			@Validated @RequestBody PaymentSyncRequest request) {
		return orderService.syncPayment(orderId, request.paymentId());
	}

	public record PaymentSyncRequest(
			@NotBlank @Pattern(regexp = "^\\d{1,20}$", message = "Pagamento inválido.") String paymentId) {
	}
}
