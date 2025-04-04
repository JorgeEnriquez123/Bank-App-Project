package com.jorge.accounts.webclient.client;

import com.jorge.accounts.webclient.model.CustomerResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class CustomerClient {
    private final WebClient webClient;

    public CustomerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080/customers").build();
    }

    public Mono<CustomerResponse> getCustomerByDni(String dni) {
        return webClient.get()
                .uri("/dni/" + dni)
                .retrieve()
                .bodyToMono(CustomerResponse.class);
    }
}
