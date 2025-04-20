package com.jorge.accounts.webclient.config;

import com.jorge.accounts.webclient.client.CreditClient;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public CustomerClient customerClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                         @Value("${web.baseurl.customerservice}") String baseUrl) {
        return new CustomerClient(webClientBuilder, circuitBreakerFactory, baseUrl);
    }

    @Bean
    public TransactionClient transactionClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                               @Value("${web.baseurl.transactionservice}") String baseUrl) {
        return new TransactionClient(webClientBuilder, circuitBreakerFactory, baseUrl);
    }

    @Bean
    public CreditClient creditClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                     @Value("${web.baseurl.creditservice}") String baseUrl) {
        return new CreditClient(webClientBuilder, circuitBreakerFactory, baseUrl);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json");  //Ejemplo header global
    }
}
