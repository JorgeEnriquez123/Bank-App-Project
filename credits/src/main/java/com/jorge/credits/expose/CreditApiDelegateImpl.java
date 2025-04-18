package com.jorge.credits.expose;

import com.jorge.credits.api.CreditsApiDelegate;
import com.jorge.credits.model.*;
import com.jorge.credits.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditApiDelegateImpl implements CreditsApiDelegate {
    private final CreditService creditService;

    @Override
    public Mono<CreditResponse> createCredit(Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
        return creditRequest.flatMap(creditService::createCredit);
    }

    @Override
    public Mono<Void> deleteCreditById(String id, ServerWebExchange exchange) {
        return creditService.deleteCreditById(id);
    }

    @Override
    public Flux<CreditResponse> getAllCredits(ServerWebExchange exchange) {
        return creditService.getAllCredits();
    }

    @Override
    public Mono<CreditResponse> getCreditById(String id, ServerWebExchange exchange) {
        return creditService.getCreditById(id);
    }

    @Override
    public Flux<CreditResponse> getCreditsByCreditHolderId(String creditHolderId, ServerWebExchange exchange) {
        return creditService.getCreditsByCreditHolderId(creditHolderId);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String id, ServerWebExchange exchange) {
        return creditService.getTransactionsByCreditId(id);
    }

    @Override
    public Mono<CreditResponse> payCreditById(String id, Mono<CreditPaymentRequest> creditPaymentRequest, ServerWebExchange exchange) {
        return creditPaymentRequest.flatMap(
                creditPaymentRequest1 -> creditService.payCreditById(id, creditPaymentRequest1));
    }

    @Override
    public Mono<CreditResponse> updateCreditById(String id, Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
        return creditRequest.flatMap(
                creditRequest1 -> creditService.updateCreditById(id, creditRequest1));
    }

    @Override
    public Mono<CreditResponse> payCreditByIdWithDebitCard(String id, Mono<CreditPaymentByDebitCardRequest> creditPaymentByDebitCardRequest, ServerWebExchange exchange) {
        return creditPaymentByDebitCardRequest.flatMap(
                request -> creditService.payCreditByIdWithDebitCard(id, request)
        );
    }
}
