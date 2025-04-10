package com.jorge.accounts.webclient.client;

import com.jorge.accounts.webclient.model.CreditCardResponse;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CreditClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public CreditClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Flux<CreditCardResponse> getCreditCardsByCardHolderId(String cardHolderId) {
        return circuitBreakerFactory.create("creditClient").run(webClient.get()
                .uri("/credit-cards/customer/" + cardHolderId)
                .retrieve()
                .bodyToFlux(CreditCardResponse.class),
                throwable -> Flux.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Credit service unavailable", throwable)));
    }
}
