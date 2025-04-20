package com.jorge.credits.webclient.client;

import com.jorge.credits.webclient.dto.response.AccountBalanceResponse;
import com.jorge.credits.webclient.dto.request.AccountBalanceUpdateRequest;
import com.jorge.credits.webclient.dto.response.AccountResponse;
import com.jorge.credits.webclient.dto.response.DebitCardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
public class AccountClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public AccountClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Mono<AccountBalanceResponse> reduceBalanceByAccountNumber(String accountNumber,
                                                                     AccountBalanceUpdateRequest accountBalanceUpdateRequest) {
        return circuitBreakerFactory.create("accountClient").run(webClient.patch()
                .uri("/accounts/account-number/" + accountNumber + "/balance/reduction")
                .bodyValue(accountBalanceUpdateRequest)
                .retrieve()
                .bodyToMono(AccountBalanceResponse.class),
                throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable)));
    }

    public Mono<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        Mono<AccountResponse> accountResponseMono = webClient.get()
                .uri("/accounts/account-number/" + accountNumber)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, ex ->
                        Mono.empty());

        return circuitBreakerFactory.create("accountClient").run(accountResponseMono,
                throwable -> {
                    log.error(throwable.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable));
                });
    }

    public Mono<DebitCardResponse> getDebitCardByDebitCardNumber(String debitCardNumber) {
        Mono<DebitCardResponse> debitCardResponseMono = webClient.get()
                .uri("/debit-cards/card-number/" + debitCardNumber)
                .retrieve()
                .bodyToMono(DebitCardResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, ex ->
                        Mono.empty());

        return circuitBreakerFactory.create("accountClient").run(debitCardResponseMono,
                throwable -> {
                    log.error(throwable.getMessage());
                    return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service unavailable", throwable));
                });
    }
}
