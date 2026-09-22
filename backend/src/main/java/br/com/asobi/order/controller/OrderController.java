package br.com.asobi.order.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.order.dto.OrderCreatedResponse;
import br.com.asobi.order.dto.OrderRequest;
import br.com.asobi.order.dto.OrderStatusResponse;
import br.com.asobi.order.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderCreatedResponse placeOrder(@Validated @RequestBody OrderRequest request) {
		return orderService.placeOrder(request);
	}

	@GetMapping("/{orderId}")
	public OrderStatusResponse getOrder(@PathVariable UUID orderId) {
		return orderService.getOrderStatus(orderId);
	}
}
