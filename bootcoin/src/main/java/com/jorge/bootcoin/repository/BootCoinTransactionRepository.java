package com.jorge.bootcoin.repository;

import com.jorge.bootcoin.model.BootCoinTransaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BootCoinTransactionRepository extends ReactiveMongoRepository<BootCoinTransaction, String> {
}
