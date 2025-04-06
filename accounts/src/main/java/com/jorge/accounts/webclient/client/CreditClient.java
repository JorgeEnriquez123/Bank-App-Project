package com.jorge.accounts.webclient.client;

import com.jorge.accounts.webclient.model.CreditCardResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public class CreditClient {
    private final WebClient webClient;

    public CreditClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
    }

    public Flux<CreditCardResponse> getCreditCardsByCardHolderId(String cardHolderId) {
        return webClient.get()
                .uri("/credit-cards/customer/" + cardHolderId)
                .retrieve()
                .bodyToFlux(CreditCardResponse.class);
    }
}
