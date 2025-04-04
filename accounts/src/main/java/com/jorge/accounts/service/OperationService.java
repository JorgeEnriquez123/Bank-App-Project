package com.jorge.accounts.service;

import com.jorge.accounts.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface OperationService {
    Mono<AccountResponse> depositByAccountNumber(String accountNumber, DepositRequest depositRequest);
    Mono<AccountResponse> withdrawByAccountNumber(String accountNumber, WithdrawalRequest withdrawalRequest);

    Mono<BalanceResponse> getBalanceByAccountNumber(String accountNumber);
    Mono<BalanceResponse> increaseBalanceByAccountNumber(String accountNumber, BigDecimal balance);
    Mono<BalanceResponse> decreaseBalanceByAccountNumber(String accountNumber, BigDecimal balance);

    Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);
}
