package com.jorge.credits.webclient.client;

import com.jorge.credits.webclient.dto.response.CustomerResponse;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public CustomerClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Mono<CustomerResponse> getCustomerById(String customerId) {
        return circuitBreakerFactory.create("customerClient")
                        .run(webClient.get()
                                .uri("/" + customerId)
                                .retrieve()
                                .bodyToMono(CustomerResponse.class),
                                throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service unavailable", throwable)));
    }
}
