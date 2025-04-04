package com.jorge.transactions.service;

import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest);
    Mono<TransactionResponse> getTransactionById(String id);
    Mono<TransactionResponse> updateTransaction(String id, TransactionRequest transactionRequest);
    Mono<Void> deleteTransactionById(String id);

    Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);
    Flux<TransactionResponse> getTransactionsByCreditId(String creditId);
}
