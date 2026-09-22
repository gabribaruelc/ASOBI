package br.com.asobi.order.service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.order.dto.OrderCreatedResponse;
import br.com.asobi.order.dto.OrderRequest;
import br.com.asobi.order.dto.OrderStatusResponse;
import br.com.asobi.order.model.Customer;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderItem;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.model.ShippingAddress;
import br.com.asobi.order.repository.OrderRepository;
import br.com.asobi.payment.PaymentGateway;
import br.com.asobi.payment.PaymentUpdate;
import br.com.asobi.shipping.ShippingOption;
import br.com.asobi.shipping.ShippingService;

@Service
@Transactional
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final PaymentGateway paymentGateway;
	private final ShippingService shippingService;
	private final Clock clock;

	public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
			PaymentGateway paymentGateway, ShippingService shippingService, Clock clock) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.paymentGateway = paymentGateway;
		this.shippingService = shippingService;
		this.clock = clock;
	}

	/**
	 * Cria o pedido com preços do banco e confere o estoque. O estoque só é
	 * baixado quando o pagamento é aprovado (ver {@link #applyPayment}).
	 */
	public OrderCreatedResponse placeOrder(OrderRequest request) {
		Map<String, Integer> quantities = mergeQuantities(request.items());
		Map<String, Product> products = productRepository.findBySlugIn(quantities.keySet()).stream()
				.collect(Collectors.toMap(Product::getSlug, Function.identity()));

		Order order = new Order(toCustomer(request.customer()), toAddress(request.shippingAddress()));
		quantities.forEach((slug, quantity) -> {
			Product product = products.get(slug);
			if (product == null) {
				throw new BusinessException("Um dos produtos do carrinho não está mais disponível. Atualize o carrinho.");
			}
			if (product.getStock() <= 0) {
				throw new BusinessException("\"" + product.getName() + "\" esgotou. Remova do carrinho para continuar.");
			}
			if (product.getStock() < quantity) {
				throw new BusinessException("Só temos " + product.getStock() + " unidade(s) de \"" + product.getName()
						+ "\". Ajuste a quantidade no carrinho.");
			}
			order.addItem(product, quantity);
		});

		// Frete automático ligado: recota e usa o preço do servidor para a opção escolhida.
		if (shippingService.isEnabled()) {
			if (!StringUtils.hasText(request.shippingOptionId())) {
				throw new BusinessException("Escolha uma opção de frete.");
			}
			ShippingOption shipping = shippingService.resolve(order.getShippingAddress().getPostalCode(),
					quantities, request.shippingOptionId());
			order.setShipping(shipping.price(), shipping.label());
		}

		orderRepository.save(order);
		String checkoutUrl = paymentGateway.createCheckout(order);
		order.recordPayment(paymentGateway.name(), null, null, null);
		log.info("Pedido #{} criado ({} itens, total {})", order.getId(), order.getItems().size(), order.getTotal());
		return new OrderCreatedResponse(order.getPublicId(), order.getId(), order.getTotal(), checkoutUrl);
	}

	@Transactional(readOnly = true)
	public OrderStatusResponse getOrderStatus(UUID publicId) {
		return OrderStatusResponse.from(orderRepository.findByPublicId(publicId)
				.orElseThrow(() -> new NotFoundException("Pedido não encontrado.")));
	}

	/**
	 * Aplica o que o provedor de pagamento informou. Idempotente: o webhook
	 * pode chegar várias vezes, mas o estoque só baixa na primeira aprovação.
	 */
	public void applyPayment(PaymentUpdate update) {
		Order order = orderRepository.findByPublicId(update.orderPublicId())
				.orElseThrow(() -> new NotFoundException("Pedido não encontrado: " + update.orderPublicId()));
		order.recordPayment(paymentGateway.name(), update.paymentId(), update.status(), update.method());

		switch (update.outcome()) {
			case APPROVED -> confirmPayment(order);
			case FAILED -> {
				if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
					order.cancel();
				}
			}
			case PENDING -> {
				// Pix/boleto aguardando: nada muda até aprovar ou expirar.
			}
		}
	}

	/**
	 * Cliente voltou do Mercado Pago com o id do pagamento: consulta o
	 * pagamento na API (não confia na URL) e aplica, se for deste pedido.
	 * Garante a atualização mesmo quando o webhook atrasa ou não chega (ex.: localhost).
	 */
	public OrderStatusResponse syncPayment(UUID publicId, String paymentId) {
		PaymentUpdate update = paymentGateway.fetchPayment(paymentId)
				.orElseThrow(() -> new BusinessException("Pagamento não encontrado."));
		if (!update.orderPublicId().equals(publicId)) {
			throw new BusinessException("Esse pagamento não pertence a este pedido.");
		}
		applyPayment(update);
		return getOrderStatus(publicId);
	}

	/** "Marcar como pago" no painel (Pix direto, transferência, modo manual). */
	public void markPaidManually(Long id) {
		Order order = getForAdmin(id);
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new BusinessException("Esse pedido não está aguardando pagamento.");
		}
		order.recordPayment("manual", null, "approved", "manual");
		confirmPayment(order);
	}

	public void markShipped(Long id, String trackingCode) {
		getForAdmin(id).markShipped(StringUtils.hasText(trackingCode) ? trackingCode.trim() : null, clock.instant());
	}

	public void cancel(Long id) {
		getForAdmin(id).cancel();
	}

	@Transactional(readOnly = true)
	public List<Order> listForAdmin(OrderStatus status) {
		return status == null ? orderRepository.findAllByOrderByCreatedAtDesc()
				: orderRepository.findByStatusOrderByCreatedAtDesc(status);
	}

	@Transactional(readOnly = true)
	public Order getForAdmin(Long id) {
		return orderRepository.findWithItemsById(id)
				.orElseThrow(() -> new NotFoundException("Pedido não encontrado."));
	}

	@Transactional(readOnly = true)
	public long countByStatus(OrderStatus status) {
		return orderRepository.countByStatus(status);
	}

	private void confirmPayment(Order order) {
		if (!order.markPaid(clock.instant())) {
			return;
		}
		for (OrderItem item : order.getItems()) {
			if (item.getProduct() == null) {
				continue;
			}
			Product product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElse(null);
			if (product == null) {
				continue;
			}
			if (product.getStock() >= item.getQuantity()) {
				product.setStock(product.getStock() - item.getQuantity());
			} else {
				// Dois clientes pagaram o último item: zera e avisa a Donna no painel.
				product.setStock(0);
				order.flagStockIssue();
				log.warn("Pedido #{} pago sem estoque suficiente de {}", order.getId(), item.getProductSlug());
			}
		}
		log.info("Pedido #{} pago", order.getId());
	}

	private static Map<String, Integer> mergeQuantities(List<OrderRequest.ItemRequest> items) {
		Map<String, Integer> quantities = new LinkedHashMap<>();
		for (OrderRequest.ItemRequest item : items) {
			quantities.merge(item.slug().trim(), item.quantity(), Integer::sum);
		}
		quantities.values().stream().filter(q -> q > 20).findAny().ifPresent(q -> {
			throw new BusinessException("Quantidade máxima é 20 por produto.");
		});
		return quantities;
	}

	private static Customer toCustomer(OrderRequest.CustomerRequest customer) {
		return new Customer(customer.name().trim(), customer.email().trim().toLowerCase(Locale.ROOT),
				customer.phone().trim());
	}

	private static ShippingAddress toAddress(OrderRequest.AddressRequest address) {
		String digits = address.postalCode().replaceAll("\\D", "");
		return new ShippingAddress(
				digits.substring(0, 5) + "-" + digits.substring(5),
				address.street().trim(),
				address.number().trim(),
				StringUtils.hasText(address.complement()) ? address.complement().trim() : null,
				address.district().trim(),
				address.city().trim(),
				address.state().trim().toUpperCase(Locale.ROOT));
	}
}
