package com.jorge.credits.webclient.client;

import com.jorge.credits.mapper.CreditCardTransactionRequest;
import com.jorge.credits.model.CreditCardTransactionResponse;
import com.jorge.credits.model.TransactionResponse;
import com.jorge.credits.webclient.model.TransactionRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionClient {
    private final WebClient webClient;

    public TransactionClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
    }

    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId) {
        return webClient.get()
                .uri("/transactions/credit-id/" + creditId)
                .retrieve()
                .bodyToFlux(TransactionResponse.class);
    }

    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest){
        return webClient.post()
                .uri("/transactions")
                .bodyValue(transactionRequest)
                .retrieve()
                .bodyToMono(TransactionResponse.class);
    }

    public Mono<CreditCardTransactionResponse> createCreditCardTransaction(CreditCardTransactionRequest creditCardTransactionRequest){
        return webClient.post()
                .uri("/credit-card-transactions")
                .bodyValue(creditCardTransactionRequest)
                .retrieve()
                .bodyToMono(CreditCardTransactionResponse.class);
    }
}
