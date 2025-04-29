package com.jorge.transactions.service.impl;

import com.jorge.transactions.mapper.TransactionMapper;
import com.jorge.transactions.model.FeeReportResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumberAndDateRange(String accountNumber,
                                                                                LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions for account number: {} and created at: {}", accountNumber, startDate);
        return transactionRepository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAt(accountNumber, startDate, endDate)
                .map(transactionMapper::mapToTransactionResponse);
    }

    @Override
    public Flux<FeeReportResponse> getTransactionsFeesByAccountNumberAndDateRange(String accountNumber,
                                                                                  LocalDateTime createdAtStart, LocalDateTime createdAtEnd) {
        log.info("Fetching transactions fees for account number: {} and created at: {}", accountNumber, createdAtStart);
        return transactionRepository.findByAccountNumberAndFeeGreaterThanAndCreatedAtBetweenOrderByCreatedAt(accountNumber,
                        BigDecimal.ZERO, createdAtStart, createdAtEnd)
                .map(transaction -> {
                    FeeReportResponse feeReport = new FeeReportResponse();
                    feeReport.setAmount(transaction.getFee());

                    if (Objects.requireNonNull(transaction.getTransactionType()) == Transaction.TransactionType.MAINTENANCE_FEE) {
                        feeReport.setType(FeeReportResponse.TypeEnum.MAINTENANCE_FEE);
                    } else {
                        feeReport.setType(FeeReportResponse.TypeEnum.TRANSACTION_FEE);
                    }

                    feeReport.setDate(transaction.getCreatedAt());
                    return feeReport;
                });
    }

    public Transaction updateTransactionFromRequest(Transaction existingTransaction, TransactionRequest transactionRequest) {
        Transaction updatedTransaction = transactionMapper.mapToTransaction(transactionRequest);
        updatedTransaction.setId(existingTransaction.getId());
        updatedTransaction.setCreatedAt(existingTransaction.getCreatedAt());
        return updatedTransaction;
    }
}
