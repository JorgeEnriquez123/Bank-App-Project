package com.jorge.bootcoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class BootcoinApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootcoinApplication.class, args);
	}

}
