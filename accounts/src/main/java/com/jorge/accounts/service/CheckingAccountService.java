package com.jorge.accounts.service;

import com.jorge.accounts.model.CheckingAccountRequest;
import com.jorge.accounts.model.CheckingAccountResponse;
import reactor.core.publisher.Mono;

public interface CheckingAccountService {
    Mono<CheckingAccountResponse> createCheckingAccount(CheckingAccountRequest checkingAccountRequest);
    Mono<CheckingAccountResponse> updateCheckingAccountByAccountNumber(String accountNumber, CheckingAccountRequest checkingAccountRequest);
}
