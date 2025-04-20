package com.jorge.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(p -> p
						.path("/accounts/**")
						.uri("lb://ACCOUNT-SERVICE")
				)
				.route(p -> p
						.path("/debit-cards/**")
						.uri("lb://ACCOUNT-SERVICE")
				)
				.route(p -> p
						.path("/credits/**")
						.uri("lb://CREDIT-SERVICE")
				)
				.route(p -> p
						.path("/credit-cards/**")
						.uri("lb://CREDIT-SERVICE")
				)
				.route(p -> p
						.path("/customers/**")
						.uri("lb://CUSTOMER-SERVICE")
				)
				.route(p -> p
						.path("/transactions/**")
						.uri("lb://TRANSACTION-SERVICE")
				)
				.route(p -> p
						.path("/credit-card-transactions/**")
						.uri("lb://TRANSACTION-SERVICE")
				)
				.route(p -> p
						.path("/yanki-wallets/**")
						.uri("lb://YANKI-SERVICE")
				)
				.build();
	}
}
