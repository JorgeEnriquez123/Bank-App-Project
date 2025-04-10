package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.FixedTermAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.FixedTermAccount;
import com.jorge.accounts.model.FixedTermAccountRequest;
import com.jorge.accounts.model.FixedTermAccountResponse;
import com.jorge.accounts.repository.FixedTermAccountRepository;
import com.jorge.accounts.service.FixedTermAccountService;
import com.jorge.accounts.utils.AccountUtils;
import com.jorge.accounts.utils.CustomerValidation;
import com.jorge.accounts.webclient.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedTermAccountServiceImpl implements FixedTermAccountService {
    private final CustomerClient customerClient;
    private final AccountUtils accountUtils;
    private final FixedTermAccountRepository fixedTermAccountRepository;
    private final FixedTermAccountMapper fixedTermAccountMapper;
    private final CustomerValidation customerValidation;

    @Override
    public Mono<FixedTermAccountResponse> createFixedTermAccount(FixedTermAccountRequest fixedTermAccountRequest) {
        log.info("Creating a new account for customer DNI: {}", fixedTermAccountRequest.getCustomerDni());
        return customerClient.getCustomerByDni(fixedTermAccountRequest.getCustomerDni())
                .flatMap(customer -> {
                    if(customer.getIsVIP() || customer.getIsPYME())
                        return customerValidation.validateCreditCardExists(customer);  // Validate if customer has Credit Cards
                    else
                        return customerValidation.validateIfCustomerHasOverDueDebt(customer);   // Validate if Customer has over due debts
                })
                .flatMap(customer -> switch (customer.getCustomerType()) {
                    case PERSONAL -> customerValidation.personalCustomerValidation(customer, Account.AccountType.FIXED_TERM)
                            .then(Mono.just(customer));
                    case BUSINESS -> customerValidation.businessCustomerValidation(Account.AccountType.FIXED_TERM)
                            .then(Mono.just(customer));
                })
                .flatMap(customer ->
                        fixedTermAccountRepository.save(fixedTermAccountMapper.mapToFixedTermAccount(fixedTermAccountRequest)))
                .flatMap(fixedTermAccount ->
                        accountUtils.handleInitialDeposit(fixedTermAccount, fixedTermAccountRequest.getBalance()))
                .map(fixedTermAccountMapper::mapToFixedTermAccountResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with dni: " + fixedTermAccountRequest.getCustomerDni() + " not found")));
    }

    @Override
    public Mono<FixedTermAccountResponse> updateFixedTermAccountByAccountNumber(String accountNumber, FixedTermAccountRequest fixedTermAccountRequest) {
        log.info("Updating FixedTerm account with account number: {}", accountNumber);
        return fixedTermAccountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fixed Term Account with account number: " + accountNumber + " not found")))
                .flatMap(existingFixedTermAccount -> fixedTermAccountRepository.save(
                        updateFixedTermAccountFromRequest(existingFixedTermAccount, fixedTermAccountRequest)))
                .map(fixedTermAccountMapper::mapToFixedTermAccountResponse);
    }

    private FixedTermAccount updateFixedTermAccountFromRequest(FixedTermAccount existingFixedTermAccount, FixedTermAccountRequest fixedTermAccountRequest) {
        FixedTermAccount fixedTermAccount = fixedTermAccountMapper.mapToFixedTermAccount(fixedTermAccountRequest);
        fixedTermAccount.setAccountNumber(existingFixedTermAccount.getAccountNumber());
        fixedTermAccount.setId(existingFixedTermAccount.getId());
        fixedTermAccount.setCreatedAt(existingFixedTermAccount.getCreatedAt());
        return fixedTermAccount;
    }
}
