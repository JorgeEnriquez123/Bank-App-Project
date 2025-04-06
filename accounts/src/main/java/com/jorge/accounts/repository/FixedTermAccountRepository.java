package com.jorge.accounts.repository;

import com.jorge.accounts.model.FixedTermAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface FixedTermAccountRepository extends ReactiveMongoRepository<FixedTermAccount, String> {
    Mono<FixedTermAccount> findByAccountNumber(String accountNumber);
}
