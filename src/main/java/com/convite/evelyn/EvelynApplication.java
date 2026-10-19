package com.convite.evelyn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EvelynApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvelynApplication.class, args);
	}

}
