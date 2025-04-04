package com.jorge.transactions.service;

import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import com.jorge.transactions.model.UpdateTransactionStatusRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);
    Mono<TransactionResponse> getTransactionById(String id);
    Flux<TransactionResponse> getTransactionsByCreditId(String creditId);
    Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest);
    Mono<TransactionResponse> updateTransactionStatusByTransactionId(String transactionId, UpdateTransactionStatusRequest updateTransactionStatusRequest);
}
