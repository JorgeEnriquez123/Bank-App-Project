package com.jorge.credits.service;

import com.jorge.credits.model.BalanceResponse;
import com.jorge.credits.model.CreditRequest;
import com.jorge.credits.model.CreditResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditService {
    Flux<CreditResponse> getAllCredits();
    Flux<CreditResponse> getCreditsByCustomerDni(String customerDni);
    Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber);

    Mono<CreditResponse> createCredit(CreditRequest creditRequest);
    Mono<Void> deleteCreditById(String id);
    Mono<CreditResponse> updateCreditById(String id, CreditRequest creditRequest);
}
