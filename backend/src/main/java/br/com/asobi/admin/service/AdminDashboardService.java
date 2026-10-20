package br.com.asobi.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.asobi.catalog.dto.ProductViewCount;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.catalog.repository.ProductViewRepository;
import br.com.asobi.common.time.StoreTime;
import br.com.asobi.order.dto.ProductSales;
import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.OrderStatus;
import br.com.asobi.order.repository.OrderRepository;
import br.com.asobi.review.model.ReviewStatus;
import br.com.asobi.review.repository.ReviewRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

	public static final List<Integer> PERIODS = List.of(7, 30, 90);
	public static final int DEFAULT_PERIOD = 30;

	private static final int RANKING_SIZE = 5;

	private final ProductRepository productRepository;
	private final ReviewRepository reviewRepository;
	private final OrderRepository orderRepository;
	private final ProductViewRepository productViewRepository;
	private final Clock clock;

	public AdminDashboardService(ProductRepository productRepository, ReviewRepository reviewRepository,
			OrderRepository orderRepository, ProductViewRepository productViewRepository, Clock clock) {
		this.productRepository = productRepository;
		this.reviewRepository = reviewRepository;
		this.orderRepository = orderRepository;
		this.productViewRepository = productViewRepository;
		this.clock = clock;
	}

	public DashboardSummary summary() {
		return new DashboardSummary(
				productRepository.count(),
				productRepository.countByStock(0),
				productRepository.countByOriginalPriceIsNotNull(),
				reviewRepository.countByStatus(ReviewStatus.PENDING),
				orderRepository.countByStatus(OrderStatus.PAID),
				orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT));
	}

	/** Jogos com estoque entre 1 e o limite das configurações (os esgotados já têm o contador próprio). */
	public List<Product> lowStock(int threshold) {
		return threshold <= 0 ? List.of() : productRepository.findByStockBetweenOrderByStockAscNameAsc(1, threshold);
	}

	/** Últimos {@code days} dias, contando hoje, no fuso da loja. O faturamento conta pela data do pagamento. */
	public DashboardMetrics metrics(int days) {
		LocalDate today = LocalDate.now(clock.withZone(StoreTime.ZONE));
		LocalDate firstDay = today.minusDays(days - 1L);
		Instant from = firstDay.atStartOfDay(StoreTime.ZONE).toInstant();

		Map<LocalDate, BigDecimal> revenueByDay = new LinkedHashMap<>();
		for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
			revenueByDay.put(day, BigDecimal.ZERO);
		}
		List<Order> paidOrders = orderRepository.findPaidSince(from);
		BigDecimal revenue = BigDecimal.ZERO;
		for (Order order : paidOrders) {
			revenue = revenue.add(order.getTotal());
			revenueByDay.merge(LocalDate.ofInstant(order.getPaidAt(), StoreTime.ZONE), order.getTotal(), BigDecimal::add);
		}
		BigDecimal averageTicket = paidOrders.isEmpty()
				? BigDecimal.ZERO
				: revenue.divide(BigDecimal.valueOf(paidOrders.size()), 2, RoundingMode.HALF_UP);

		BigDecimal bestDay = revenueByDay.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
		List<DayRevenue> daily = new ArrayList<>();
		revenueByDay.forEach((day, amount) -> daily.add(new DayRevenue(day, amount, percentOf(amount, bestDay))));

		PageRequest top = PageRequest.of(0, RANKING_SIZE);
		return new DashboardMetrics(
				days,
				revenue,
				paidOrders.size(),
				orderRepository.countByCreatedAtGreaterThanEqual(from),
				averageTicket,
				productViewRepository.sumViewsSince(firstDay),
				bestDay,
				daily,
				orderRepository.findBestSellersSince(from, top),
				productViewRepository.findMostViewedSince(firstDay, top));
	}

	private static int percentOf(BigDecimal amount, BigDecimal max) {
		if (max.signum() == 0) {
			return 0;
		}
		return amount.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
	}

	public record DashboardSummary(long products, long outOfStock, long onSale, long pendingReviews,
			long ordersToShip, long ordersAwaitingPayment) {
	}

	/**
	 * @param paidOrders   pedidos com pagamento aprovado no período
	 * @param placedOrders pedidos feitos no período, pagos ou não
	 * @param bestDay      maior faturamento de um único dia (topo do gráfico)
	 */
	public record DashboardMetrics(int days, BigDecimal revenue, long paidOrders, long placedOrders,
			BigDecimal averageTicket, long views, BigDecimal bestDay, List<DayRevenue> daily,
			List<ProductSales> bestSellers, List<ProductViewCount> mostViewed) {
	}

	/** @param percent altura da barra, de 0 a 100, em relação ao melhor dia */
	public record DayRevenue(LocalDate date, BigDecimal revenue, int percent) {
	}
}
