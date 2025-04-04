package com.jorge.accounts.webclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private String accountNumber;
    //Credit field
    private String creditId;

    private BigDecimal fee;
    private TransactionType transactionType;
    private CurrencyType currencyType;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;

    public enum TransactionType {
        DEBIT,
        CREDIT,
        DEPOSIT,
        WITHDRAWAL,
        CREDIT_PAYMENT,
        CREDIT_DEPOSIT,
        CREDIT_CARD_CONSUMPTION,
        CREDIT_CARD_PAYMENT,
        TRANSACTION_FEE,
        MAINTENANCE_FEE
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public enum CurrencyType{
        PEN, USD
    }
}
