package br.com.asobi.newsletter.dto;

import java.math.BigDecimal;

/**
 * Um jogo em destaque na newsletter, com o preço do momento do envio.
 *
 * @param imageUrl      capa do jogo; sem foto, o e-mail mostra o {@code icon}
 * @param originalPrice preço cheio, só quando o jogo está em promoção
 */
public record NewsletterProductCard(Long id, String name, String url, String imageUrl, String icon,
		BigDecimal price, BigDecimal originalPrice) {
}
