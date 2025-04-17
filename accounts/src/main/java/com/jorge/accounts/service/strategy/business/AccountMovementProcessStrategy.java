package com.jorge.accounts.service.strategy.business;

import com.jorge.accounts.model.Account;
import reactor.core.publisher.Mono;

public interface AccountMovementProcessStrategy {
    Mono<Account> processMovement(Account account);
    Account.AccountType getAccountType();
}
