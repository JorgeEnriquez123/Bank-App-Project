package com.jorge.credits.webclient.config;

import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public TransactionClient transactionClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return new TransactionClient(webClientBuilder, circuitBreakerFactory);
    }

    @Bean
    public CustomerClient customerClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return new CustomerClient(webClientBuilder, circuitBreakerFactory);
    }

    @Bean
    public AccountClient accountClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        return new AccountClient(webClientBuilder, circuitBreakerFactory);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json");
    }
}
