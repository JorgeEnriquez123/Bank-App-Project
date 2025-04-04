package com.jorge.transactions.repository;

import com.jorge.transactions.model.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findByAccountNumber(String accountNumber);
    Flux<Transaction> findByCreditId(String creditId);
}
