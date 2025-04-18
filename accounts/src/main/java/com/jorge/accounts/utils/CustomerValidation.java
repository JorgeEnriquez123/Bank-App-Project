package com.jorge.accounts.utils;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.webclient.client.CreditClient;
import com.jorge.accounts.webclient.dto.response.CustomerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerValidation {
    private final AccountRepository accountRepository;
    private final CreditClient creditClient;

    public Mono<Account> personalCustomerValidation(CustomerResponse customer, Account.AccountType accountType) {
        log.info("Validating personal customer account creation for Id: {}, Account Type: {}", customer.getId(), accountType);
        return accountRepository.findByCustomerIdAndAccountType(customer.getId(), accountType)
                .flatMap(account -> {
                    log.warn("Conflict: Customer with Id: {} already has a {} account", customer.getId(), accountType);
                    return Mono.<Account>error(new ResponseStatusException(HttpStatus.CONFLICT,
                            "Customer with dni: " + customer.getId() + " already has a " + accountType.name() + " account"));
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

    public Mono<CustomerResponse> validateCreditCardExists(CustomerResponse customerResponse){
        log.info("Validating if customer with Id: {} has credit cards", customerResponse.getId());
        return creditClient.getCreditCardsByCardHolderId(customerResponse.getId())
                .hasElements()
                .flatMap(hasCards -> {
                    if (hasCards) {
                        log.info("Customer with Id: {} has credit cards", customerResponse.getId());
                        return Mono.just(customerResponse);
                    } else {
                        log.warn("Customer with Id: {} does not have credit cards", customerResponse.getId());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer does not own a credit card. " +
                                "VIP and PYME need a credit card to create an account"));
                    }
                });
    }

    public Mono<CustomerResponse> validateIfCustomerHasOverDueDebt(CustomerResponse customerResponse) {
        log.info("Validating if customer with Id: {} has overdue debts", customerResponse.getId());
        return creditClient.getCreditsByCreditHolderId(customerResponse.getId())
                .any(credit -> credit.getDueDate().isBefore(LocalDate.now()))
                .flatMap(hasOverdueDebt -> {
                    if (hasOverdueDebt) {
                        log.warn("Customer with Id: {} has overdue debt", customerResponse.getId());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Customer with dni: " + customerResponse.getDni() + " has overdue debts"));
                    } else {
                        log.info("Customer with Id: {} has no overdue debts", customerResponse.getId());
                        return Mono.just(customerResponse);
                    }
                });
    }

}
