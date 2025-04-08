package com.jorge.credits.webclient.client;

import com.jorge.credits.webclient.model.CustomerResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;

    public CustomerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/customers").build();
    }

    public Mono<CustomerResponse> getCustomerById(String customerId) {
        return webClient.get()
                .uri("/" + customerId)
                .retrieve()
                .bodyToMono(CustomerResponse.class);
    }
}
