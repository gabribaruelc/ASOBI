package br.com.asobi.alert;

/** Os avisos que a loja oferece; os mesmos nomes do notification-service. */
public enum AlertType {
	PROMOTION, BACK_IN_STOCK,
	/** "Está acabando": o jogo chegou ao limite de estoque baixo. */
	LOW_STOCK
}
