package com.jorge.accounts.webclient.client;

import com.jorge.accounts.model.TransactionResponse;
import com.jorge.accounts.webclient.model.TransactionRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class TransactionClient {
    private final WebClient webClient;

    public TransactionClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082/transactions").build();
    }

    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        return webClient.get()
                .uri("/account-number/" + accountNumber)
                .retrieve()
                .bodyToFlux(TransactionResponse.class);
    }

    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest){
        return webClient.post()
                .bodyValue(transactionRequest)
                .retrieve()
                .bodyToMono(TransactionResponse.class);
    }

    public Flux<TransactionResponse> findByAccountNumberAndCreatedAtBetweenOrderByCreatedAt(String accountNumber,
                                                                                            LocalDateTime startOfMonth,
                                                                                            LocalDateTime endOfMonth) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/account-number/" + accountNumber + "/by-date-range")
                        .queryParam("firstDayOfMonth", startOfMonth)
                        .queryParam("lastDayOfMonth", endOfMonth)
                        .build())
                .retrieve()
                .bodyToFlux(TransactionResponse.class);
    }
}
