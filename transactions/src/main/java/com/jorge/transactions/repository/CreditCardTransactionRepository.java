package com.jorge.transactions.repository;

import com.jorge.transactions.model.CreditCardTransaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CreditCardTransactionRepository extends ReactiveMongoRepository<CreditCardTransaction, String> {
    Flux<CreditCardTransaction> findByCreditCardNumber(String creditCardNumber);
}
