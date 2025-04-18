package com.jorge.accounts.webclient.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private String accountNumber;
    private BigDecimal fee;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;

    private String relatedCreditId;

    public enum TransactionType {
        DEBIT,
        CREDIT,
        DEPOSIT,
        WITHDRAWAL,
        CREDIT_PAYMENT,
        CREDIT_DEPOSIT,
        CREDIT_CARD_PAYMENT,
        MAINTENANCE_FEE
    }
}
