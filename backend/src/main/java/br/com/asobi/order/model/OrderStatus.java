package br.com.asobi.order.model;

public enum OrderStatus {

	PENDING_PAYMENT("Aguardando pagamento"),
	PAID("Pago — preparar envio"),
	SHIPPED("Enviado"),
	CANCELED("Cancelado");

	private final String label;

	OrderStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
