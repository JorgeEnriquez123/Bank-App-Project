package com.jorge.credits.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_cards")
public class CreditCard {
    @Id
    private String id;
    private String cardHolderId;
    private CreditCardType type;
    private String creditCardNumber;
    private String cvv;
    private LocalDate expiryDate;
    private CreditCardStatus status;
    private BigDecimal creditLimit;
    private LocalDateTime createdAt;

    private BigDecimal availableBalance;    // What you have left in your credit card
    private BigDecimal outstandingBalance;  // What you have consumed on your credit card

    public enum CreditCardType {
        PERSONAL_CREDIT_CARD,
        BUSINESS_CREDIT_CARD
    }

    public enum CreditCardStatus {
        ACTIVE,
        BLOCKED
    }
}
