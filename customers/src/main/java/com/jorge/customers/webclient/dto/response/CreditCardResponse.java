package com.jorge.customers.webclient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

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
