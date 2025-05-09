package com.jorge.accounts.service;

import com.jorge.accounts.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DebitCardService {
    Flux<DebitCardResponse> getAllDebitCards();
    Mono<DebitCardResponse> getDebitCardById(String id);
    Mono<DebitCardResponse> getDebitCardByDebitCardNumber(String debitCardNumber);
    Flux<DebitCardResponse> getDebitCardsByCardHolderId(String cardHolderId);
    Mono<DebitCardResponse> createDebitCard(DebitCardRequest debitCardRequest);
    Mono<DebitCardResponse> updateDebitCardByDebitCardNumber(String debitCardNumber, DebitCardRequest debitCardRequest);
    Mono<Void> deleteDebitCardByDebitCardNumber(String debitCardNumber);

    Mono<BalanceResponse> withdrawByDebitCardNumber(String debitCardNumber, WithdrawalRequest withdrawalRequest);

    Mono<BalanceResponse> getBalanceByDebitCardNumber(String debitCardNumber);

    Flux<TransactionResponse> getTransactionsByDebitCardNumberLast10(String debitCardNumber);
}
