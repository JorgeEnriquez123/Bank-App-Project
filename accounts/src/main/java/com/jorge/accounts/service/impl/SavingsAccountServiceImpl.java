package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.AccountMapper;
import com.jorge.accounts.mapper.SavingsAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.SavingsAccount;
import com.jorge.accounts.model.SavingsAccountRequest;
import com.jorge.accounts.model.SavingsAccountResponse;
import com.jorge.accounts.repository.AccountRepository;
import com.jorge.accounts.repository.SavingsAccountRepository;
import com.jorge.accounts.service.SavingsAccountService;
import com.jorge.accounts.utils.AccountUtils;
import com.jorge.accounts.utils.CustomerTypeValidation;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.model.CustomerResponse;
import com.jorge.accounts.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsAccountServiceImpl implements SavingsAccountService {
    private final CustomerClient customerClient;
    private final AccountUtils accountUtils;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountMapper savingsAccountMapper;
    private final CustomerTypeValidation customerTypeValidation;

    @Override
    public Mono<SavingsAccountResponse> createSavingsAccount(SavingsAccountRequest savingsAccountRequest) {
        log.info("Creating a new account for customer DNI: {}", savingsAccountRequest.getCustomerDni());
        return customerClient.getCustomerByDni(savingsAccountRequest.getCustomerDni())
                .flatMap(customer -> {
                    if(customer.getIsVIP() || customer.getIsPYME()) // If Customer is VIP or PYME, perform validations
                        return customerTypeValidation.validateCreditCardExists(customer);  // Validate if customer has Credit Cards
                    else
                        return Mono.just(customer);
                })
                .flatMap(customer -> switch (customer.getCustomerType()) {
                    case PERSONAL -> customerTypeValidation.personalCustomerValidation(customer, Account.AccountType.SAVINGS)
                            .then(Mono.just(customer));
                    case BUSINESS -> customerTypeValidation.businessCustomerValidation(Account.AccountType.SAVINGS)
                            .then(Mono.just(customer));
                })
                .flatMap(customer ->
                        savingsAccountRepository.save(savingsAccountMapper.mapToSavingsAccount(savingsAccountRequest)))
                .flatMap(checkingAccount ->
                        accountUtils.handleInitialDeposit(checkingAccount, savingsAccountRequest.getBalance()))
                .map(savingsAccountMapper::mapToSavingsAccountResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with dni: " + savingsAccountRequest.getCustomerDni() + " not found")));
    }

    @Override
    public Mono<SavingsAccountResponse> updateSavingsAccountByAccountNumber(String accountNumber, SavingsAccountRequest savingsAccountRequest) {
        log.info("Updating Savings account with account number: {}", accountNumber);
        return savingsAccountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Savings Account with account number: " + accountNumber + " not found")))
                .flatMap(existingSavingsAccount -> savingsAccountRepository.save(
                        updateSavingsAccountFromRequest(existingSavingsAccount, savingsAccountRequest)))
                .map(savingsAccountMapper::mapToSavingsAccountResponse);
    }

    private SavingsAccount updateSavingsAccountFromRequest(SavingsAccount existingSavingsAccount, SavingsAccountRequest savingsAccountRequest) {
        SavingsAccount savingsAccount = savingsAccountMapper.mapToSavingsAccount(savingsAccountRequest);
        savingsAccount.setAccountNumber(existingSavingsAccount.getAccountNumber());
        savingsAccount.setId(existingSavingsAccount.getId());
        savingsAccount.setCreatedAt(existingSavingsAccount.getCreatedAt());
        return savingsAccount;
    }

}
