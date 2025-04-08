package com.jorge.accounts.service;

import com.jorge.accounts.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AccountService {
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountResponse> getAccountByAccountNumber(String accountNumber);
    Mono<Void> deleteAccountByAccountNumber(String accountNumber);
    Mono<BalanceResponse> getBalanceByAccountNumber(String accountNumber);

    Mono<BalanceResponse> increaseBalanceByAccountNumber(String accountNumber, BigDecimal balance);
    Mono<BalanceResponse> decreaseBalanceByAccountNumber(String accountNumber, BigDecimal balance);
    Mono<AccountResponse> depositByAccountNumber(String accountNumber, DepositRequest depositRequest);
    Mono<AccountResponse> withdrawByAccountNumber(String accountNumber, WithdrawalRequest withdrawalRequest);
    Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber);

    Mono<AverageMonthlyDailyBalanceResponse> calculateAverageMonthlyDailyBalance(String accountNumber, AverageMonthlyDailyBalanceRequest averageMonthlyDailyBalanceRequest);
    Mono<TransactionResponse> transfer (String accountNumber, TransferRequest transferRequest);
    Flux<FeeReportResponse> generateFeeReportBetweenDate(String accountNumber, LocalDateTime startDate, LocalDateTime endDate);
}
