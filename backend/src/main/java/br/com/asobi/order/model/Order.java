package br.com.asobi.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.asobi.catalog.model.Product;
import br.com.asobi.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identificador usado fora do painel (link de acompanhamento, Mercado Pago). Não dá para adivinhar. */
	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId = UUID.randomUUID();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Embedded
	private Customer customer;

	@Embedded
	private ShippingAddress shippingAddress;

	/** Conta do cliente (Supabase Auth) quando o pedido foi feito logado; null = sem cadastro. */
	@Column(name = "customer_user_id")
	private UUID customerUserId;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@Column(name = "items_total", nullable = false, precision = 10, scale = 2)
	private BigDecimal itemsTotal = BigDecimal.ZERO;

	@Column(name = "shipping_cost", nullable = false, precision = 10, scale = 2)
	private BigDecimal shippingCost = BigDecimal.ZERO;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	/** Serviço de entrega escolhido (ex.: "Correios PAC · 5 dias úteis"); null = frete a combinar. */
	@Column(name = "shipping_service", length = 120)
	private String shippingService;

	@Column(name = "payment_provider", length = 30)
	private String paymentProvider;

	@Column(name = "payment_id", length = 64)
	private String paymentId;

	@Column(name = "payment_status", length = 40)
	private String paymentStatus;

	@Column(name = "payment_method", length = 40)
	private String paymentMethod;

	@Column(name = "stock_issue", nullable = false)
	private boolean stockIssue;

	@Column(name = "tracking_code", length = 60)
	private String trackingCode;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Column(name = "shipped_at")
	private Instant shippedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Order() {
	}

	public Order(Customer customer, ShippingAddress shippingAddress) {
		this.customer = customer;
		this.shippingAddress = shippingAddress;
	}

	public void assignToCustomer(UUID customerUserId) {
		this.customerUserId = customerUserId;
	}

	public void addItem(Product product, int quantity) {
		OrderItem item = new OrderItem(this, product, quantity);
		items.add(item);
		itemsTotal = itemsTotal.add(item.getLineTotal());
		total = itemsTotal.add(shippingCost);
	}

	public void setShippingCost(BigDecimal shippingCost) {
		this.shippingCost = shippingCost;
		this.total = itemsTotal.add(shippingCost);
	}

	public void setShipping(BigDecimal shippingCost, String shippingService) {
		setShippingCost(shippingCost);
		this.shippingService = shippingService;
	}

	/** Registra o que o provedor de pagamento informou (sem mudar o status do pedido). */
	public void recordPayment(String provider, String paymentId, String paymentStatus, String paymentMethod) {
		this.paymentProvider = provider;
		this.paymentId = paymentId;
		this.paymentStatus = paymentStatus;
		this.paymentMethod = paymentMethod;
	}

	/** @return true se mudou de status agora (para baixar o estoque só uma vez). */
	public boolean markPaid(Instant when) {
		if (status == OrderStatus.PAID || status == OrderStatus.SHIPPED) {
			return false;
		}
		this.status = OrderStatus.PAID;
		this.paidAt = when;
		return true;
	}

	public void markShipped(String trackingCode, Instant when) {
		if (status != OrderStatus.PAID) {
			throw new BusinessException("Só pedidos pagos podem ser marcados como enviados.");
		}
		this.status = OrderStatus.SHIPPED;
		this.trackingCode = trackingCode;
		this.shippedAt = when;
	}

	public void cancel() {
		if (status != OrderStatus.PENDING_PAYMENT) {
			throw new BusinessException("Só pedidos aguardando pagamento podem ser cancelados por aqui.");
		}
		this.status = OrderStatus.CANCELED;
	}

	public void flagStockIssue() {
		this.stockIssue = true;
	}

	public Long getId() {
		return id;
	}

	public UUID getPublicId() {
		return publicId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Customer getCustomer() {
		return customer;
	}

	public UUID getCustomerUserId() {
		return customerUserId;
	}

	public ShippingAddress getShippingAddress() {
		return shippingAddress;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public BigDecimal getItemsTotal() {
		return itemsTotal;
	}

	public BigDecimal getShippingCost() {
		return shippingCost;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public String getShippingService() {
		return shippingService;
	}

	public String getPaymentProvider() {
		return paymentProvider;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public String getPaymentStatus() {
		return paymentStatus;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public boolean isStockIssue() {
		return stockIssue;
	}

	public String getTrackingCode() {
		return trackingCode;
	}

	public Instant getPaidAt() {
		return paidAt;
	}

	public Instant getShippedAt() {
		return shippedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
