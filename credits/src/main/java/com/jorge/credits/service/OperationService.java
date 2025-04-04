package com.jorge.credits.service;

import com.jorge.credits.model.CreditPaymentRequest;
import com.jorge.credits.model.CreditResponse;
import com.jorge.credits.model.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface OperationService {
    Flux<TransactionResponse> getTransactionsByCreditId(String creditId);

    Mono<CreditResponse> payCreditById(String creditId, CreditPaymentRequest creditPaymentRequest);
    Mono<CreditResponse> payCreditCardByCreditCardNumber(String creditCardNumber, CreditPaymentRequest creditPaymentRequest);

    Mono<CreditResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, BigDecimal amount);
}
