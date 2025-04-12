package com.jorge.customers.webclient.config;

import com.jorge.customers.webclient.client.AccountClient;
import com.jorge.customers.webclient.client.CreditClient;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public AccountClient accountClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return new AccountClient(webClientBuilder, circuitBreakerFactory);
    }

    @Bean
    public CreditClient creditClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return new CreditClient(webClientBuilder, circuitBreakerFactory);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json");
    }
}
