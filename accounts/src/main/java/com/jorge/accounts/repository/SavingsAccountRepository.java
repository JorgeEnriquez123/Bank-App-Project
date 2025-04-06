package com.jorge.accounts.repository;

import com.jorge.accounts.model.SavingsAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SavingsAccountRepository extends ReactiveMongoRepository<SavingsAccount, String> {
    Mono<SavingsAccount> findByAccountNumber(String accountNumber);
}
