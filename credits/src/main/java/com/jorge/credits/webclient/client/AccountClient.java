package com.jorge.credits.webclient.client;

import com.jorge.credits.webclient.model.AccountBalanceResponse;
import com.jorge.credits.webclient.model.AccountBalanceUpdateRequest;
import com.jorge.credits.webclient.model.CustomerResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AccountClient {
    private final WebClient webClient;

    public AccountClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081/accounts").build();
    }

    public Mono<AccountBalanceResponse> reduceBalanceByAccountNumber(String accountNumber,
                                                                     AccountBalanceUpdateRequest accountBalanceUpdateRequest) {
        return webClient.patch()
                .uri("/account-number/" + accountNumber + "/balance/reduction")
                .bodyValue(accountBalanceUpdateRequest)
                .retrieve()
                .bodyToMono(AccountBalanceResponse.class);
    }
}
