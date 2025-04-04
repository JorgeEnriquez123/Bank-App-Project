package com.jorge.transactions.mapper;

import com.jorge.transactions.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreditCardTransactionMapper {
    public CreditCardTransaction mapToCreditCardTransaction(CreditCardTransactionRequest creditCardTransactionRequest) {
        CreditCardTransaction creditCardTransaction = new CreditCardTransaction();
        creditCardTransaction.setCreditCardNumber(creditCardTransactionRequest.getCreditCardNumber());
        creditCardTransaction.setTransactionType(CreditCardTransaction.CreditCardTransactionType.valueOf(
                creditCardTransactionRequest.getTransactionType().name()));
        creditCardTransaction.setAmount(creditCardTransactionRequest.getAmount());
        creditCardTransaction.setCreatedAt(LocalDateTime.now());
        return creditCardTransaction;
    }

    public CreditCardTransactionResponse mapToCreditCardTransactionResponse(CreditCardTransaction creditCardTransaction) {
        CreditCardTransactionResponse creditCardTransactionResponse = new CreditCardTransactionResponse();
        creditCardTransactionResponse.setId(creditCardTransaction.getId());
        creditCardTransactionResponse.setCreditCardNumber(creditCardTransaction.getCreditCardNumber());
        creditCardTransactionResponse.setTransactionType(CreditCardTransactionResponse.TransactionTypeEnum.valueOf(
                creditCardTransaction.getTransactionType().name()));
        creditCardTransactionResponse.setAmount(creditCardTransaction.getAmount());
        creditCardTransactionResponse.setCreatedAt(creditCardTransaction.getCreatedAt());
        return creditCardTransactionResponse;
    }
}
