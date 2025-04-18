package com.jorge.accounts.utils;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.webclient.client.TransactionClient;
import com.jorge.accounts.webclient.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class AccountUtils {
    private final TransactionClient transactionClient;

    private static final int ACCOUNT_NUMBER_LENGTH = 14;

    // To keep it simple
    public String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();

        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    public <T extends Account> Mono<T> handleInitialDeposit(T account, BigDecimal initialBalance) {
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            TransactionRequest transactionRequest = new TransactionRequest();
            transactionRequest.setAccountNumber(account.getAccountNumber());
            transactionRequest.setAmount(initialBalance);
            transactionRequest.setTransactionType(TransactionRequest.TransactionType.DEPOSIT);
            transactionRequest.setDescription("Account opening balance " + initialBalance);
            transactionRequest.setFee(account.getIsCommissionFeeActive() ?
                    account.getMovementCommissionFee() : BigDecimal.ZERO);
            return transactionClient.createTransaction(transactionRequest)
                    .thenReturn(account);
        } else {
            return Mono.just(account);
        }
    }
}
