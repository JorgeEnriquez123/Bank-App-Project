package com.jorge.credits.webclient.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardTransactionRequest {
    private String creditCardNumber;
    private CreditCardTransactionType transactionType;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public enum CreditCardTransactionType {
        CREDIT_CARD_CONSUMPTION,
        CREDIT_CARD_PAYMENT,
    }
}
