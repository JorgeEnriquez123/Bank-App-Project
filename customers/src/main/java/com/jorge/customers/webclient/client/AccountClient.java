package com.jorge.customers.webclient.client;

import com.jorge.customers.webclient.dto.response.AccountResponse;
import com.jorge.customers.webclient.dto.response.DebitCardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

public class AccountClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public AccountClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Flux<AccountResponse> getAccountsByCustomerId(String customerId){
        return circuitBreakerFactory.create("accountClient").run(webClient.get()
                        .uri("/accounts/customer/" + customerId)
                        .retrieve()
                        .bodyToFlux(AccountResponse.class),
                throwable -> Flux.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable)));
    }

    public Flux<DebitCardResponse> getDebitCardsByCardHolderId(String cardHolderId){
        return circuitBreakerFactory.create("accountClient").run(webClient.get()
                        .uri("/debit-cards/card-holder/" + cardHolderId)
                        .retrieve()
                        .bodyToFlux(DebitCardResponse.class),
                throwable -> Flux.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable)));
    }
}
