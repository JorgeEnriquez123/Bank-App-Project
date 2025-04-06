package com.jorge.accounts.webclient.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardResponse {
    private String id;
    private String cardHolderId;
    private CreditCardType type;
    private String creditCardNumber;
    private String cvv;
    private LocalDate expiryDate;
    private CreditCardStatus status;
    private String creditLimit;

    private BigDecimal availableBalance; // What you have left in your credit card
    private BigDecimal outstandingBalance; // What you have consumed on your credit card

    public enum CreditCardType {
        PERSONAL_CREDIT_CARD,
        BUSINESS_CREDIT_CARD
    }

    public enum CreditCardStatus {
        ACTIVE,
        BLOCKED
    }
}
