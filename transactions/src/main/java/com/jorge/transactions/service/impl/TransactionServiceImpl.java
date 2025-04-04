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
public class TransactionServiceImpl implements TransactionService {
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    @Override
    public Flux<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Mono<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        log.info("Creating a new transaction");
        Transaction transaction = transactionMapper.mapToTransaction(transactionRequest);
        return transactionRepository.save(transaction)
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
    public Mono<TransactionResponse> updateTransaction(String id, TransactionRequest transactionRequest) {
        log.info("Updating transaction status for transaction id: {}", id);
        Mono<Transaction> transactionMono = transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movimiento con id: " + id + " no encontrado")));
        return transactionMono.flatMap(existingTransaction ->
                                transactionRepository.save(
                                        updateTransactionFromRequest(existingTransaction, transactionRequest))
                ).map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Mono<Void> deleteTransactionById(String id) {
        log.info("Deleting transaction by id: {}", id);
        return transactionRepository.deleteById(id);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber) {
        log.info("Fetching transactions for account number: {}", accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Transactions not found for account number: " + accountNumber)))
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

    public Transaction updateTransactionFromRequest(Transaction existingTransaction, TransactionRequest transactionRequest) {
        existingTransaction.setAccountNumber(transactionRequest.getAccountNumber());
        existingTransaction.setFee(transactionRequest.getFee());
        existingTransaction.setTransactionType(Transaction.TransactionType.valueOf(transactionRequest.getTransactionType().name()));
        existingTransaction.setAmount(transactionRequest.getAmount());
        existingTransaction.setDescription(transactionRequest.getDescription());
        existingTransaction.setRelatedCreditId(transactionRequest.getRelatedCreditId());
        return existingTransaction;
    }
}
