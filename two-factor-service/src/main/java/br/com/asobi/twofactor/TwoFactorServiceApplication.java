package br.com.asobi.twofactor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TwoFactorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwoFactorServiceApplication.class, args);
	}

}
