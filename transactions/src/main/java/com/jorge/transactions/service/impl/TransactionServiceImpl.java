package com.jorge.transactions.service.impl;

import com.jorge.transactions.mapper.TransactionMapper;
import com.jorge.transactions.model.Transaction;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import com.jorge.transactions.repository.TransactionRepository;
import com.jorge.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl {
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    /*@Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        log.info("Fetching transactions for account number: {}", accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Transactions not found for account number: " + accountNumber)))
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Mono<TransactionResponse> getTransactionById(String id) {
        log.info("Fetching transaction by id: {}", id);
        return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movimiento con id: " + id + " no encontrado")))
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId) {
        log.info("Fetching transactions for credit id: {}", creditId);
        return transactionRepository.findByRelatedCreditId((creditId))
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Transactions not found for credit id: " + creditId)))
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        log.info("Creating a new transaction");
        Transaction transaction = transactionMapper.mapToTransaction(transactionRequest);
        transaction.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction)
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Mono<TransactionResponse> updateTransactionById(String transactionId, UpdateTransactionStatusRequest updateTransactionStatusRequest) {
        log.info("Updating transaction status for transaction id: {}", transactionId);
        Mono<Transaction> transactionMono = transactionRepository.findById(transactionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movimiento con id: " + transactionId + " no encontrado")));
        return transactionMono.flatMap(transactionRepository::save)
                .map(transactionMapper::mapToTransactionResponse);
    }*/
}
