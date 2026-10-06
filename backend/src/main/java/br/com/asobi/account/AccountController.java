package br.com.asobi.account;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.order.dto.OrderStatusResponse;
import br.com.asobi.order.service.OrderService;

/** Área do cliente logado ("Meus pedidos"). */
@RestController
@RequestMapping("/api/account")
public class AccountController {

	private final CustomerAuth customerAuth;
	private final OrderService orderService;

	public AccountController(CustomerAuth customerAuth, OrderService orderService) {
		this.customerAuth = customerAuth;
		this.orderService = orderService;
	}

	@GetMapping("/orders")
	public List<OrderStatusResponse> listOrders(
			@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return orderService.listForCustomer(customerAuth.require(authorization));
	}
}
