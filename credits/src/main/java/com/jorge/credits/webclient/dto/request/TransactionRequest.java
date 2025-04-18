package com.jorge.credits.webclient.dto.request;

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
        CREDIT_PAYMENT,
        CREDIT_DEPOSIT,
        CREDIT_CARD_CONSUMPTION,
        CREDIT_CARD_PAYMENT
    }
}