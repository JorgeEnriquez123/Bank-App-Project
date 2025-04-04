package com.jorge.transactions.mapper;

import com.jorge.transactions.model.Transaction;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionMapper {
    public Transaction mapToTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(transactionRequest.getAccountNumber());
        transaction.setFee(transactionRequest.getFee());
        transaction.setTransactionType(Transaction.TransactionType.valueOf(transactionRequest.getTransactionType().name()));
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDescription(transactionRequest.getDescription());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setRelatedCreditId(transactionRequest.getRelatedCreditId());
        return transaction;
    }

    public TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setId(transaction.getId());
        transactionResponse.setAccountNumber(transaction.getAccountNumber());
        transactionResponse.setFee(transaction.getFee());
        transactionResponse.setTransactionType(TransactionResponse.TransactionTypeEnum.valueOf(transaction.getTransactionType().name()));
        transactionResponse.setAmount(transaction.getAmount());
        transactionResponse.setDescription(transaction.getDescription());
        transactionResponse.setCreatedAt(transaction.getCreatedAt());
        transactionResponse.setRelatedCreditId(transaction.getRelatedCreditId());
        return transactionResponse;
    }
}
