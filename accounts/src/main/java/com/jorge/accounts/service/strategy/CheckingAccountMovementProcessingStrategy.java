package com.jorge.accounts.service.strategy;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.service.strategy.business.AccountMovementProcessStrategy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CheckingAccountMovementProcessingStrategy implements AccountMovementProcessStrategy {
    @Override
    public Mono<Account> processMovement(Account account) {
        return Mono.just(account);
    }

    @Override
    public Account.AccountType getAccountType() {
        return Account.AccountType.CHECKING;
    }
}
