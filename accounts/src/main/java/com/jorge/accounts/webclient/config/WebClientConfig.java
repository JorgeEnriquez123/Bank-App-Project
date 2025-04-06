package com.jorge.accounts.webclient.config;

import com.jorge.accounts.webclient.client.CreditClient;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public CustomerClient customerClient(WebClient.Builder webClientBuilder) {
        return new CustomerClient(webClientBuilder);
    }

    @Bean
    public TransactionClient transactionClient(WebClient.Builder webClientBuilder) {
        return new TransactionClient(webClientBuilder);
    }

    @Bean
    public CreditClient creditClient(WebClient.Builder webClientBuilder) {
        return new CreditClient(webClientBuilder);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json");  //Ejemplo header global
    }
}
