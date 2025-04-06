package com.jorge.accounts.service.impl;

import com.jorge.accounts.mapper.CheckingAccountMapper;
import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.CheckingAccount;
import com.jorge.accounts.model.CheckingAccountRequest;
import com.jorge.accounts.model.CheckingAccountResponse;
import com.jorge.accounts.repository.CheckingAccountRepository;
import com.jorge.accounts.service.CheckingAccountService;
import com.jorge.accounts.utils.CustomerTypeValidation;
import com.jorge.accounts.webclient.client.CustomerClient;
import com.jorge.accounts.webclient.model.CustomerResponse;
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
public class CheckingAccountServiceImpl implements CheckingAccountService {
    private final CustomerClient customerClient;
    private final CheckingAccountRepository checkingAccountRepository;
    private final CheckingAccountMapper checkingAccountMapper;
    private final CustomerTypeValidation customerTypeValidation;

    @Override
    public Mono<CheckingAccountResponse> createCheckingAccount(CheckingAccountRequest checkingAccountRequest) {
        log.info("Creating a new account for customer DNI: {}", checkingAccountRequest.getCustomerDni());
        return customerClient.getCustomerByDni(checkingAccountRequest.getCustomerDni())
                .flatMap(customer -> {
                    if(customer.getIsVIP() || customer.getIsPYME())
                        return customerTypeValidation.validateCreditCardExists(customer);  // Validate if customer has Credit Cards
                    else
                        return Mono.just(customer);
                })
                .flatMap(customer -> switch (customer.getCustomerType()) {
                    case PERSONAL -> customerTypeValidation.personalCustomerValidation(customer, Account.AccountType.CHECKING)
                            .then(Mono.just(customer));
                    case BUSINESS -> customerTypeValidation.businessCustomerValidation(Account.AccountType.CHECKING)
                            .then(Mono.just(customer));
                })
                .flatMap(customer ->{
                    CheckingAccount checkingAccount = checkingAccountMapper.mapToCheckingAccount(checkingAccountRequest);
                    // If Customer is PYME, Checking Account has no maintenance fee
                    if(customer.getIsPYME()) checkingAccount.setMaintenanceFee(BigDecimal.ZERO);
                    return checkingAccountRepository.save(checkingAccount);
                })
                .map(checkingAccountMapper::mapToCheckingAccountResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with dni: " + checkingAccountRequest.getCustomerDni() + " not found")));
    }

    @Override
    public Mono<CheckingAccountResponse> updateCheckingAccountByAccountNumber(String accountNumber, CheckingAccountRequest checkingAccountRequest) {
        log.info("Updating Checking account with account number: {}", accountNumber);
        return checkingAccountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Checking Account with account number: " + accountNumber + " not found")))
                .flatMap(existingCheckingAccount -> checkingAccountRepository.save(
                        updateCheckingAccountFromRequest(existingCheckingAccount, checkingAccountRequest)))
                .map(checkingAccountMapper::mapToCheckingAccountResponse);
    }

    private CheckingAccount updateCheckingAccountFromRequest(CheckingAccount existingCheckingAccount, CheckingAccountRequest checkingAccountRequest) {
        CheckingAccount checkingAccount = checkingAccountMapper.mapToCheckingAccount(checkingAccountRequest);
        checkingAccount.setAccountNumber(existingCheckingAccount.getAccountNumber());
        checkingAccount.setId(existingCheckingAccount.getId());
        checkingAccount.setCreatedAt(existingCheckingAccount.getCreatedAt());
        return checkingAccount;
    }
}
