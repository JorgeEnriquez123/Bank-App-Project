package com.jorge.yanki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class YankiApplication {

	public static void main(String[] args) {
		SpringApplication.run(YankiApplication.class, args);
	}

}
