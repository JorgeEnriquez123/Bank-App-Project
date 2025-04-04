package com.jorge.transactions.mapper;

import com.jorge.transactions.model.Transaction;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionMapper {
    public Transaction mapToTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(transactionRequest.getAccountNumber());
        transaction.setTransactionType(Transaction.TransactionType.valueOf(transactionRequest.getTransactionType().name()));
        transaction.setCurrencyType(Transaction.CurrencyType.valueOf(transactionRequest.getCurrencyType().name()));
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setStatus(Transaction.TransactionStatus.valueOf(transactionRequest.getStatus().name()));
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setFee(transactionRequest.getFee());

        switch (transactionRequest.getTransactionType()) {
            case CREDIT_DEPOSIT, CREDIT_PAYMENT, CREDIT_CARD_CONSUMPTION, CREDIT_CARD_PAYMENT ->
                    transaction.setCreditId(transactionRequest.getCreditId());
        }
        return transaction;
    }

    public TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setId(transaction.getId());
        transactionResponse.setAccountNumber(transaction.getAccountNumber());
        transactionResponse.setCreditId(transaction.getCreditId());
        transactionResponse.setFee(transaction.getFee());
        transactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.valueOf(transaction.getTransactionType().name()));
        transactionResponse.setCurrencyType(TransactionResponse.CurrencyTypeEnum.valueOf(transaction.getCurrencyType().name()));
        transactionResponse.setAmount(transaction.getAmount());
        transactionResponse.setStatus(TransactionResponse.StatusEnum.valueOf(transaction.getStatus().name()));
        transactionResponse.setDescription(transaction.getDescription());
        transactionResponse.setTransactionDate(transaction.getTransactionDate());
        return transactionResponse;
    }
}
