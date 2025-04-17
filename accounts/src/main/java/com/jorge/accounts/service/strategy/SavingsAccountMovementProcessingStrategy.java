package com.jorge.accounts.service.strategy;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.SavingsAccount;
import com.jorge.accounts.service.strategy.business.AccountMovementProcessStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class SavingsAccountMovementProcessingStrategy implements AccountMovementProcessStrategy {
    @Override
    public Mono<Account> processMovement(Account account){
        SavingsAccount savingsAccount = (SavingsAccount) account;
        if(savingsAccount.getMovementsThisMonth() >= savingsAccount.getMonthlyMovementsLimit()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Monthly movement limit reached for savings account"));
        }
        return Mono.just(account);
    }

    @Override
    public Account.AccountType getAccountType() {
        return Account.AccountType.SAVINGS;
    }
}
