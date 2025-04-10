package com.jorge.accounts.webclient.client;

import com.jorge.accounts.webclient.model.CustomerResponse;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public CustomerClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/customers").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Mono<CustomerResponse> getCustomerByDni(String dni) {
        return circuitBreakerFactory.create("customerClient").run(webClient.get()
                .uri("/dni/" + dni)
                .retrieve()
                .bodyToMono(CustomerResponse.class),
                throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer service unavailable", throwable)));
    }
}
