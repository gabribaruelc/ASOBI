package br.com.asobi.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microsserviço de frete: recebe origem, destino e volumes e devolve as opções
 * de entrega cotadas na transportadora (hoje: Melhor Envio). Não tem banco nem
 * regra comercial (frete grátis, toggle do painel) — isso fica no backend da loja.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ShippingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShippingServiceApplication.class, args);
	}

}
