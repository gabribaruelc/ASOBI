package br.com.asobi.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import br.com.asobi.order.model.Order;
import br.com.asobi.order.model.ShippingAddress;

/** Monta os e-mails de pedido (assunto + HTML do template email/order.html). */
@Component
public class OrderEmailComposer {

	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final ITemplateEngine templateEngine;
	private final String storeUrl;
	private final String publicUrl;

	public OrderEmailComposer(ITemplateEngine templateEngine,
			@Value("${asobi.store-url}") String storeUrl,
			@Value("${asobi.public-url}") String publicUrl) {
		this.templateEngine = templateEngine;
		this.storeUrl = storeUrl.replaceAll("/+$", "");
		this.publicUrl = publicUrl.replaceAll("/+$", "");
	}

	/** Para o cliente: pedido recebido, aguardando pagamento. */
	public EmailMessage placed(Order order) {
		Context context = customerContext(order, "Recebemos seu pedido! 🎲", List.of(
				"Oi, " + firstName(order) + "! Seu pedido nº " + order.getId() + " chegou aqui na ASOBI.",
				"Assim que o pagamento for confirmado, a gente avisa por e-mail e começa a preparar o envio."));
		return message(order, "Recebemos seu pedido nº " + order.getId() + " — ASOBI", context);
	}

	/** Para o cliente: pagamento aprovado. */
	public EmailMessage paid(Order order) {
		Context context = customerContext(order, "Pagamento confirmado! 🎉", List.of(
				"Oi, " + firstName(order) + "! Recebemos o pagamento do pedido nº " + order.getId() + ".",
				"Já estamos separando os jogos com carinho. Você recebe outro e-mail quando o pedido for enviado."));
		return message(order, "Pagamento confirmado: pedido nº " + order.getId() + " — ASOBI", context);
	}

	/** Para o cliente: pedido enviado (com o código de rastreio, se houver). */
	public EmailMessage shipped(Order order) {
		Context context = customerContext(order, "Seu pedido está a caminho! 📦", List.of(
				"Oi, " + firstName(order) + "! O pedido nº " + order.getId() + " saiu para entrega.",
				"Boa brincadeira quando ele chegar!"));
		if (StringUtils.hasText(order.getTrackingCode())) {
			context.setVariable("highlight", "Código de rastreio: " + order.getTrackingCode());
		}
		return message(order, "Seu pedido nº " + order.getId() + " foi enviado — ASOBI", context);
	}

	/** Para os admins: chegou um pedido pago, hora de separar e enviar. */
	public EmailMessage paidAdminNotice(Order order, List<String> adminEmails) {
		List<String> paragraphs = new ArrayList<>();
		paragraphs.add(order.getCustomer().getName() + " pagou o pedido nº " + order.getId()
				+ " (" + money(order.getTotal()) + "). Já dá para separar e enviar.");
		Context context = baseContext(order, "Novo pedido pago 💰", paragraphs);
		if (order.isStockIssue()) {
			context.setVariable("highlight",
					"Atenção: o estoque não era suficiente para este pedido. Confira antes de enviar.");
		}
		context.setVariable("contact", order.getCustomer().getEmail() + " · " + order.getCustomer().getPhone());
		context.setVariable("buttonLabel", "Abrir pedido no painel");
		context.setVariable("buttonUrl", publicUrl + "/admin/pedidos/" + order.getId());
		context.setVariable("footerNote", "Aviso automático do painel ASOBI.");
		String subject = "Novo pedido pago nº " + order.getId() + " — " + money(order.getTotal());
		return new EmailMessage(adminEmails, subject, templateEngine.process("email/order", context));
	}

	private EmailMessage message(Order order, String subject, Context context) {
		return new EmailMessage(List.of(order.getCustomer().getEmail()), subject,
				templateEngine.process("email/order", context));
	}

	private Context customerContext(Order order, String heading, List<String> paragraphs) {
		Context context = baseContext(order, heading, paragraphs);
		context.setVariable("buttonLabel", "Acompanhar pedido");
		context.setVariable("buttonUrl", storeUrl + "/pedido/" + order.getPublicId());
		context.setVariable("footerNote", "Você recebeu este e-mail porque fez um pedido na ASOBI.");
		return context;
	}

	private Context baseContext(Order order, String heading, List<String> paragraphs) {
		Context context = new Context(PT_BR);
		context.setVariable("heading", heading);
		context.setVariable("paragraphs", paragraphs);
		context.setVariable("order", order);
		context.setVariable("address", addressLine(order.getShippingAddress()));
		return context;
	}

	private static String firstName(Order order) {
		return order.getCustomer().getName().trim().split("\\s+")[0];
	}

	private static String addressLine(ShippingAddress address) {
		return address.getStreet() + ", " + address.getNumber()
				+ (StringUtils.hasText(address.getComplement()) ? " (" + address.getComplement() + ")" : "")
				+ " — " + address.getDistrict() + ", " + address.getCity() + "/" + address.getState()
				+ " · CEP " + address.getPostalCode();
	}

	private static String money(BigDecimal value) {
		return "R$ " + String.format(PT_BR, "%,.2f", value);
	}
}
