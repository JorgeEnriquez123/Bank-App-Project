package com.jorge.bootcoin.service;

import com.jorge.bootcoin.tempdto.BootCoinTransactionRequest;
import com.jorge.bootcoin.tempdto.BootCoinTransactionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BootCoinTransactionService {
    Flux<BootCoinTransactionResponse> getAllBootCoinTransactions();
    Mono<BootCoinTransactionResponse> getBootCoinTransactionById(String id);
    Mono<BootCoinTransactionResponse> createBootCoinTransaction(BootCoinTransactionRequest request);
    Mono<BootCoinTransactionResponse> updateBootCoinTransaction(String id, BootCoinTransactionRequest request);
    Mono<Void> deleteBootCoinTransactionById(String id);
}
