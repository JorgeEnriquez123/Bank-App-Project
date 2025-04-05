package com.jorge.credits.service;

import com.jorge.credits.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditService {
    Flux<CreditResponse> getAllCredits();
    Mono<CreditResponse> createCredit(CreditRequest creditRequest);
    Mono<CreditResponse> getCreditById(String id);
    Mono<CreditResponse> updateCreditById(String id, CreditRequest creditRequest);
    Mono<Void> deleteCreditById(String id);
    Flux<CreditResponse> getCreditsByCreditHolderId(String creditHolderId);
    Mono<CreditResponse> payCreditById(String id, CreditPaymentRequest creditPaymentRequest);
    Flux<TransactionResponse> getTransactionsByCreditId(String id);
}
