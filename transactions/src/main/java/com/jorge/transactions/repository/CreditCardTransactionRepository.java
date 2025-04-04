package com.jorge.transactions.repository;

import com.jorge.transactions.model.CreditCardTransaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardTransactionRepository extends ReactiveMongoRepository<CreditCardTransaction, String> {
}
