package com.jorge.transactions.repository;

import com.jorge.transactions.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findByAccountNumber(String accountNumber);
    Flux<Transaction> findByRelatedCreditId(String relatedCreditId);
    Flux<Transaction> findByAccountNumberAndCreatedAtBetweenOrderByCreatedAt(String accountNumber,
                                                                             LocalDateTime firstDayOfMonth,
                                                                             LocalDateTime lastDayOfMonth);

    Flux<Transaction> findByAccountNumberAndFeeGreaterThanAndCreatedAtBetweenOrderByCreatedAt(String accountNumber,
                                                                                                      BigDecimal feeIsGreaterThan,
                                                                                                      LocalDateTime createdAtStart,
                                                                                                      LocalDateTime createdAtEnd);
}
