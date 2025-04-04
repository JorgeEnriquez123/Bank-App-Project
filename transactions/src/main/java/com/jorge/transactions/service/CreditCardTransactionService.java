package com.jorge.transactions.service;

import com.jorge.transactions.model.CreditCardTransactionRequest;
import com.jorge.transactions.model.CreditCardTransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditCardTransactionService {
    Mono<CreditCardTransactionResponse> createCreditCardTransaction(CreditCardTransactionRequest creditCardTransactionRequest);
    Mono<CreditCardTransactionResponse> getCreditCardTransactionById(String id);
    Mono<CreditCardTransactionResponse> updateCreditCardTransaction(String id, CreditCardTransactionRequest creditCardTransactionRequest);
    Mono<Void> deleteCreditCardTransactionById(String id);

    Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber);
}
