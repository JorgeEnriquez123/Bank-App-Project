package com.jorge.accounts.service;

import com.jorge.accounts.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountService {
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountResponse> getAccountByAccountNumber(String accountNumber);
    Mono<AccountResponse> createAccount(AccountRequest accountRequest);
    Mono<Void> deleteAccountByAccountNumber(String accountNumber);
    Mono<AccountResponse> updateAccountByAccountNumber(String accountNumber, AccountRequest accountRequest);
}
