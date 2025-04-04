package com.jorge.credits.webclient.client;

import com.jorge.credits.model.TransactionResponse;
import com.jorge.credits.webclient.model.TransactionRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionClient {
    private final WebClient webClient;

    public TransactionClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082/transactions").build();
    }

    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId) {
        return webClient.get()
                .uri("/credit-id/" + creditId)
                .retrieve()
                .bodyToFlux(TransactionResponse.class);
    }

    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest){
        return webClient.post()
                .bodyValue(transactionRequest)
                .retrieve()
                .bodyToMono(TransactionResponse.class);
    }
}
