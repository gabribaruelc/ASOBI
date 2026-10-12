package br.com.asobi.adminauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminAuthServiceApplication.class, args);
	}

}
