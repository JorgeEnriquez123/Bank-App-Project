package com.jorge.credits.webclient.client;

import com.jorge.credits.model.CreditCardTransactionResponse;
import com.jorge.credits.model.TransactionResponse;
import com.jorge.credits.webclient.model.CreditCardTransactionRequest;
import com.jorge.credits.webclient.model.TransactionRequest;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionClient {
    private final WebClient webClient;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public TransactionClient(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.get()
                .uri("/transactions/credit-id/" + creditId)
                .retrieve()
                .bodyToFlux(TransactionResponse.class),
                throwable -> Flux.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber) {
        return circuitBreakerFactory.create("transactionClient").run(webClient.get()
                        .uri("/credit-card-transactions/credit-card-number/" + creditCardNumber)
                        .retrieve()
                        .bodyToFlux(CreditCardTransactionResponse.class),
                throwable -> Flux.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest){
        return circuitBreakerFactory.create("transactionClient")
                .run(webClient.post()
                        .uri("/transactions")
                        .bodyValue(transactionRequest)
                        .retrieve()
                        .bodyToMono(TransactionResponse.class),
                        throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }

    public Mono<CreditCardTransactionResponse> createCreditCardTransaction(CreditCardTransactionRequest creditCardTransactionRequest){
        return circuitBreakerFactory.create("transactionClient")
                .run(webClient.post()
                        .uri("/credit-card-transactions")
                        .bodyValue(creditCardTransactionRequest)
                        .retrieve()
                        .bodyToMono(CreditCardTransactionResponse.class),
                        throwable -> Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Transaction service unavailable", throwable)));
    }
}
