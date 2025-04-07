package com.jorge.transactions.service;

import com.jorge.transactions.model.FeeReportResponse;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionService {
    Flux<TransactionResponse> getAllTransactions();
    Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest);
    Mono<TransactionResponse> getTransactionById(String id);
    Mono<TransactionResponse> updateTransaction(String id, TransactionRequest transactionRequest);
    Mono<Void> deleteTransactionById(String id);

    Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);
    Flux<TransactionResponse> getTransactionsByCreditId(String creditId);
    Flux<TransactionResponse> getTransactionsByAccountNumberAndDateRange(String accountNumber,
                                                                         LocalDateTime startDate,
                                                                         LocalDateTime endDate);
    Flux<FeeReportResponse> getTransactionsFeesByAccountNumberAndDateRange(String accountNumber,
                                                                           BigDecimal feeIsGreaterThan,
                                                                           LocalDateTime createdAtStart,
                                                                           LocalDateTime createdAtEnd);
}
