package com.jorge.accounts;

import com.jorge.accounts.api.AccountsApiDelegate;
import com.jorge.accounts.model.*;
import com.jorge.accounts.service.AccountService;
import com.jorge.accounts.service.CheckingAccountService;
import com.jorge.accounts.service.FixedTermAccountService;
import com.jorge.accounts.service.SavingsAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccountApiDelegateImpl implements AccountsApiDelegate {
    private final AccountService accountService;
    private final SavingsAccountService savingsAccountService;
    private final CheckingAccountService checkingAccountService;
    private final FixedTermAccountService fixedTermAccountService;

    @Override
    public Mono<AverageMonthlyDailyBalanceResponse> calculateAverageMonthlyDailyBalanceByAccountNumber(String accountNumber, Mono<AverageMonthlyDailyBalanceRequest> averageMonthlyDailyBalanceRequest, ServerWebExchange exchange) {
        return averageMonthlyDailyBalanceRequest.flatMap(
                request -> accountService.calculateAverageMonthlyDailyBalance(accountNumber, request));
    }

    @Override
    public Flux<FeeReportResponse> generateFeeReportBetweenDateByAccountNumber(String accountNumber, LocalDateTime startDate, LocalDateTime endDate, ServerWebExchange exchange) {
        return accountService.generateFeeReportBetweenDate(accountNumber, startDate, endDate);
    }

    @Override
    public Mono<TransactionResponse> transferByAccountNumber(String accountNumber, Mono<TransferRequest> transferRequest, ServerWebExchange exchange) {
        return AccountsApiDelegate.super.transferByAccountNumber(accountNumber, transferRequest, exchange);
    }

    @Override
    public Mono<CheckingAccountResponse> createCheckingAccount(Mono<CheckingAccountRequest> checkingAccountRequest, ServerWebExchange exchange) {
        return checkingAccountRequest.flatMap(checkingAccountService::createCheckingAccount);
    }

    @Override
    public Mono<FixedTermAccountResponse> createFixedTermAccount(Mono<FixedTermAccountRequest> fixedTermAccountRequest, ServerWebExchange exchange) {
        return fixedTermAccountRequest.flatMap(fixedTermAccountService::createFixedTermAccount);
    }

    @Override
    public Mono<SavingsAccountResponse> createSavingsAccount(Mono<SavingsAccountRequest> savingsAccountRequest, ServerWebExchange exchange) {
        return savingsAccountRequest.flatMap(savingsAccountService::createSavingsAccount);
    }

    @Override
    public Mono<Void> deleteAccountByAccountNumber(String accountNumber, ServerWebExchange exchange) {
        return accountService.deleteAccountByAccountNumber(accountNumber);
    }

    @Override
    public Mono<AccountResponse> depositByAccountNumber(String accountNumber, Mono<DepositRequest> depositRequest, ServerWebExchange exchange) {
        return depositRequest.flatMap(request -> accountService.depositByAccountNumber(accountNumber, request));
    }

    @Override
    public Mono<AccountResponse> getAccountByAccountNumber(String accountNumber, ServerWebExchange exchange) {
        return accountService.getAccountByAccountNumber(accountNumber);
    }

    @Override
    public Flux<AccountResponse> getAllAccounts(ServerWebExchange exchange) {
        return accountService.getAllAccounts();
    }

    @Override
    public Mono<BalanceResponse> getBalanceByAccountNumber(String accountNumber, ServerWebExchange exchange) {
        return accountService.getBalanceByAccountNumber(accountNumber);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber, ServerWebExchange exchange) {
        return accountService.getTransactionsByAccountNumber(accountNumber);
    }

    @Override
    public Mono<BalanceResponse> increaseBalanceByAccountNumber(String accountNumber, Mono<BalanceUpdateRequest> balanceUpdateRequest, ServerWebExchange exchange) {
        return balanceUpdateRequest.flatMap(request -> accountService.increaseBalanceByAccountNumber(accountNumber, request.getBalance()));
    }

    @Override
    public Mono<BalanceResponse> reduceBalanceByAccountNumber(String accountNumber, Mono<BalanceUpdateRequest> balanceUpdateRequest, ServerWebExchange exchange) {
        return balanceUpdateRequest.flatMap(request -> accountService.decreaseBalanceByAccountNumber(accountNumber, request.getBalance()));
    }

    @Override
    public Mono<CheckingAccountResponse> updateCheckingAccountByAccountNumber(String accountNumber, Mono<CheckingAccountRequest> checkingAccountRequest, ServerWebExchange exchange) {
        return checkingAccountRequest.flatMap(request -> checkingAccountService.updateCheckingAccountByAccountNumber(accountNumber, request));
    }

    @Override
    public Mono<FixedTermAccountResponse> updateFixedTermAccountByAccountNumber(String accountNumber, Mono<FixedTermAccountRequest> fixedTermAccountRequest, ServerWebExchange exchange) {
        return fixedTermAccountRequest.flatMap(request -> fixedTermAccountService.updateFixedTermAccountByAccountNumber(accountNumber, request));
    }

    @Override
    public Mono<SavingsAccountResponse> updateSavingsAccountByAccountNumber(String accountNumber, Mono<SavingsAccountRequest> savingsAccountRequest, ServerWebExchange exchange) {
        return savingsAccountRequest.flatMap(request -> savingsAccountService.updateSavingsAccountByAccountNumber(accountNumber, request));
    }

    @Override
    public Mono<AccountResponse> withdrawByAccountNumber(String accountNumber, Mono<WithdrawalRequest> withdrawalRequest, ServerWebExchange exchange) {
        return withdrawalRequest.flatMap(request -> accountService.withdrawByAccountNumber(accountNumber, request));
    }
}
