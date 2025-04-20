package com.jorge.accounts.webclient.client;

import com.jorge.accounts.model.FeeReportResponse;
import com.jorge.accounts.model.TransactionResponse;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class TransactionClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public TransactionClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.get()
                        .uri("/account-number/" + accountNumber)
                        .retrieve()
                        .bodyToFlux(TransactionResponse.class),
                throwable -> Flux.error(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.post()
                        .bodyValue(transactionRequest)
                        .retrieve()
                        .bodyToMono(TransactionResponse.class),
                throwable -> Mono.error(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Flux<TransactionResponse> getTransactionsByAccountNumberAndDateRange(String accountNumber,
                                                                                LocalDateTime startOfMonth,
                                                                                LocalDateTime endOfMonth) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/account-number/" + accountNumber + "/by-date-range")
                                .queryParam("firstDayOfMonth", startOfMonth)
                                .queryParam("lastDayOfMonth", endOfMonth)
                                .build())
                        .retrieve()
                        .bodyToFlux(TransactionResponse.class),
                throwable -> Flux.error(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Flux<FeeReportResponse> getTransactionsFeesByAccountNumberAndDateRange(String accountNumber,
                                                                                  LocalDateTime startDate,
                                                                                  LocalDateTime endDate) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/account-number/" + accountNumber + "/fees-by-date-range")
                                .queryParam("startDate", startDate)
                                .queryParam("endDate", endDate)
                                .build())
                        .retrieve()
                        .bodyToFlux(FeeReportResponse.class),
                throwable -> Flux.error(
                        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }
}
