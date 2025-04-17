package com.jorge.accounts.service;

import com.jorge.accounts.model.SavingsAccountRequest;
import com.jorge.accounts.model.SavingsAccountResponse;
import reactor.core.publisher.Mono;

public interface SavingsAccountService {
    // CRUD
    Mono<SavingsAccountResponse> createSavingsAccount(SavingsAccountRequest savingsAccountRequest);
    Mono<SavingsAccountResponse> updateSavingsAccountByAccountNumber(String accountNumber, SavingsAccountRequest savingsAccountRequest);
}

