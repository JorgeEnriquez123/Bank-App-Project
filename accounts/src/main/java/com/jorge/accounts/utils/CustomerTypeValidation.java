package com.jorge.accounts.utils;

import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.webclient.model.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerTypeValidation {
    private final AccountRepository accountRepository;

    public Mono<Account> personalCustomerValidation(CustomerResponse customer, Account.AccountType accountType) {
        log.info("Validating personal customer account creation for DNI: {}, Account Type: {}", customer.getDni(), accountType);
        return accountRepository.findByCustomerDniAndAccountType(customer.getDni(), accountType)
                .flatMap(account -> {
                    log.warn("Conflict: Customer with dni: {} already has a {} account", customer.getDni(), accountType);
                    return Mono.<Account>error(new ResponseStatusException(HttpStatus.CONFLICT,
                            "Customer with dni: " + customer.getDni() + " already has a " + accountType.name() + " account"));
                })
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Void> businessCustomerValidation(Account.AccountType accountType) {
        log.info("Validating business customer account creation. Account Type: {}", accountType);
        if (accountType != Account.AccountType.CHECKING) {
            log.warn("Business customer attempted to create a non-CHECKING account: {}", accountType);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Business customer can't create a " + accountType.name() + " account"));
        }
        return Mono.empty();
    }
}
