package br.com.asobi.status;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StatusServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatusServiceApplication.class, args);
	}
}
