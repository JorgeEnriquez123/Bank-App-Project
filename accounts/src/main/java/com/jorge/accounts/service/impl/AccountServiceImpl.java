package com.jorge.accounts.service.impl;

import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.model.CustomerResponse;
import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.model.*;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.service.AccountService;
import com.jorge.accounts.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final CustomerClient customerClient;
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;
    private final AccountUtils accountUtils;

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll()
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<AccountResponse> getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .map(accountMapper::mapToAccountResponse);
    }

    @Override
    public Mono<AccountResponse> createAccount(AccountRequest accountRequest) {
        log.info("Creating a new account for customer DNI: {}", accountRequest.getCustomerDni());
        Mono<CustomerResponse> customerResponse = customerClient.getCustomerByDni(accountRequest.getCustomerDni());
        return customerResponse.flatMap(customer ->
                switch (customer.getCustomerType()) {
                    case PERSONAL -> personalCustomerValidation(customer, accountRequest);
                    case BUSINESS -> businessCustomerValidation(accountRequest);
                    default -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported customer type"));
                }).map(accountMapper::mapToAccountResponse);
    }

    private Mono<Account> personalCustomerValidation(CustomerResponse customer, AccountRequest accountRequest) {
        log.info("Validating personal customer account creation for DNI: {}, Account Type: {}", customer.getDni(), accountRequest.getAccountType());
        return accountRepository.findByCustomerDniAndAccountType(customer.getDni(),
                        Account.AccountType.valueOf(accountRequest.getAccountType().name()))
                .flatMap(account -> {
                    log.warn("Conflict: Customer with DNI {} already has a {} account", customer.getDni(), accountRequest.getAccountType());
                    return Mono.<Account>error(new ResponseStatusException(HttpStatus.CONFLICT,
                        "Customer with dni: " + customer.getDni() + " already has a " + accountRequest.getAccountType().name() + " account"));
                    })
                .switchIfEmpty(Mono.defer(() -> {
                    Account newAccount = accountCreationSetUp(accountRequest);
                    log.info("Saving new personal account");
                    return accountRepository.save(newAccount);
                }));
    }

    private Mono<Account> businessCustomerValidation(AccountRequest accountRequest) {
        log.info("Validating business customer account creation. Account Type: {}", accountRequest.getAccountType());
        if (accountRequest.getAccountType() != AccountRequest.AccountTypeEnum.CHECKING) {
            log.warn("Business customer attempted to create a non-CHECKING account: {}", accountRequest.getAccountType());
            return Mono.error(
                    new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Business customer can't create a " + accountRequest.getAccountType().name() + " account"));
        }
        log.info("Saving new business account");
        return accountRepository.save(accountCreationSetUp(accountRequest));
    }

    @Override
    public Mono<Void> deleteAccountByAccountNumber(String accountNumber) {
        log.info("Deleting account with account number: {}", accountNumber);
        return accountRepository.deleteByAccountNumber(accountNumber);
    }

    @Override
    public Mono<AccountResponse> updateAccountByAccountNumber(String accountNumber, AccountRequest accountRequest) {
        log.info("Updating account with account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account with account number: " + accountNumber + " not found")))
                .flatMap(existingAccount -> {
                    Account account = accountMapper.mapToAccount(accountRequest);
                    account.setId(existingAccount.getId());
                    account.setAccountNumber(existingAccount.getAccountNumber());
                    account.setCreatedAt(existingAccount.getCreatedAt());
                    log.info("Saving updated account with account number: {}", accountNumber);
                    return accountRepository.save(account);
                    })
                .map(accountMapper::mapToAccountResponse);
    }

    public Account accountCreationSetUp(AccountRequest accountRequest) {
        Account account = accountMapper.mapToAccount(accountRequest);
        account.setAccountNumber(accountUtils.generateAccountNumber());
        account.setCreatedAt(LocalDateTime.now());
        log.info("Setting up new account with generated account number: {}", account.getAccountNumber());
        return account;
    }
}
