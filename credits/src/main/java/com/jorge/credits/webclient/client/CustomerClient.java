package com.jorge.credits.webclient.client;

import com.jorge.credits.model.TransactionResponse;
import com.jorge.credits.webclient.model.CustomerResponse;
import com.jorge.credits.webclient.model.TransactionRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;

    public CustomerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/customers").build();
    }

    public Mono<CustomerResponse> getCustomerByDni(String customerDni) {
        return webClient.get()
                .uri("/dni/" + customerDni)
                .retrieve()
                .bodyToMono(CustomerResponse.class);
    }
}
