package com.jorge.accounts.repository;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.AccountResponse;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {
    Mono<Account> findByAccountNumber(String accountNumber);
    Mono<Account> findByCustomerIdAndAccountType(String customerId, Account.AccountType accountType);
    Mono<Void> deleteByAccountNumber(String accountNumber);

    Flux<Account> findByCustomerId(String customerId);
}
