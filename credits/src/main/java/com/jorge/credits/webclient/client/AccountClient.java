package com.jorge.credits.webclient.client;

import com.jorge.credits.webclient.model.AccountBalanceResponse;
import com.jorge.credits.webclient.model.AccountBalanceUpdateRequest;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public class AccountClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public AccountClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081/accounts").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Mono<AccountBalanceResponse> reduceBalanceByAccountNumber(String accountNumber,
                                                                     AccountBalanceUpdateRequest accountBalanceUpdateRequest) {
        return circuitBreakerFactory.create("accountClient").run(webClient.patch()
                .uri("/account-number/" + accountNumber + "/balance/reduction")
                .bodyValue(accountBalanceUpdateRequest)
                .retrieve()
                .bodyToMono(AccountBalanceResponse.class),
                throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable)));
    }
}
