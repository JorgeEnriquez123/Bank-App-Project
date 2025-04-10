package com.jorge.credits.service;

import com.jorge.credits.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditCardService {
    Flux<CreditCardResponse> getAllCreditCards();
    Mono<CreditCardResponse> createCreditCard(CreditCardRequest creditCardRequest);
    Mono<CreditCardResponse> getCreditCardById(String id);
    Mono<CreditCardResponse> updateCreditCardById(String id, CreditCardRequest creditCardRequest);
    Mono<Void> deleteCreditCardById(String id);

    Flux<CreditCardResponse> getCreditCardsByCardHolderId(String cardHolderId);
    Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber);
    Mono<CreditCardResponse> payCreditCardByCreditCardNumber(String creditCardNumber, CreditPaymentRequest creditPaymentRequest);
    Mono<CreditCardResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, ConsumptionRequest consumptionRequest);

    Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber);
}
