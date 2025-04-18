package com.jorge.accounts.webclient.client;

import com.jorge.accounts.webclient.dto.response.CustomerResponse;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public CustomerClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/customers").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Mono<CustomerResponse> getCustomerById(String id) {
        Mono<CustomerResponse> customerResponseMono = webClient.get()
                .uri("/" + id)
                .retrieve()
                .bodyToMono(CustomerResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, ex ->
                        Mono.empty());

        return circuitBreakerFactory.create("customerClient").run(customerResponseMono,
                throwable ->{
                    System.out.println("Error occurred: " + throwable.getMessage());
                        return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Customer service unavailable", throwable));
        });
    }
}
