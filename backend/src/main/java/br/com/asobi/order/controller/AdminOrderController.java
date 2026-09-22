package br.com.asobi.order.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.service.OrderService;

/** Pedidos (/admin/pedidos). */
@Controller
@RequestMapping("/admin/pedidos")
public class AdminOrderController {

	/** Abas da lista: nome na URL → status (null = todos). */
	private static final Map<String, OrderStatus> TABS = new LinkedHashMap<>();

	static {
		TABS.put("para-enviar", OrderStatus.PAID);
		TABS.put("aguardando-pagamento", OrderStatus.PENDING_PAYMENT);
		TABS.put("enviados", OrderStatus.SHIPPED);
		TABS.put("cancelados", OrderStatus.CANCELED);
		TABS.put("todos", null);
	}

	private final OrderService orderService;

	public AdminOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "para-enviar") String status, Model model) {
		String tab = TABS.containsKey(status) ? status : "para-enviar";
		model.addAttribute("tab", tab);
		model.addAttribute("orders", orderService.listForAdmin(TABS.get(tab)));
		model.addAttribute("toShipCount", orderService.countByStatus(OrderStatus.PAID));
		return "admin/orders";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("order", orderService.getForAdmin(id));
		return "admin/order-detail";
	}

	@PostMapping("/{id}/marcar-pago")
	public String markPaid(@PathVariable Long id, RedirectAttributes redirect) {
		return run(id, redirect, "Pagamento confirmado. O estoque foi atualizado.",
				() -> orderService.markPaidManually(id));
	}

	@PostMapping("/{id}/marcar-enviado")
	public String markShipped(@PathVariable Long id, @RequestParam(required = false) String trackingCode,
			RedirectAttributes redirect) {
		return run(id, redirect, "Pedido marcado como enviado.", () -> orderService.markShipped(id, trackingCode));
	}

	@PostMapping("/{id}/cancelar")
	public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
		return run(id, redirect, "Pedido cancelado.", () -> orderService.cancel(id));
	}

	private static String run(Long id, RedirectAttributes redirect, String successMessage, Runnable action) {
		try {
			action.run();
			redirect.addFlashAttribute("success", successMessage);
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/pedidos/" + id;
	}
}
