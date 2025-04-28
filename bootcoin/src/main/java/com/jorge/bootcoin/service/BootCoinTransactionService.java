package com.jorge.bootcoin.service;

import com.jorge.bootcoin.model.BootCoinTransactionRequest;
import com.jorge.bootcoin.model.BootCoinTransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BootCoinTransactionService {
    Flux<BootCoinTransactionResponse> getAllBootCoinTransactions();
    Mono<BootCoinTransactionResponse> getBootCoinTransactionById(String id);
    Mono<BootCoinTransactionResponse> createBootCoinTransaction(BootCoinTransactionRequest request);
    Mono<BootCoinTransactionResponse> updateBootCoinTransaction(String id, BootCoinTransactionRequest request);
    Mono<Void> deleteBootCoinTransactionById(String id);
}
