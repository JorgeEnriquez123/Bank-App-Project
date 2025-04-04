package com.jorge.credits.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {
    private String id;
    private String customerDni;
    private CreditType creditType;
    private Status status;
    private BigDecimal creditAmount;
    private LocalDateTime createdAt;

    // Credit Card fields
    private BigDecimal availableBalance; // What you have left in your credit card
    private BigDecimal outstandingBalance; // What you have consumed on your credit card

    private String creditCardNumber;
    private String cvv;
    private LocalDate expiryDate;

    public enum CreditType {
        PERSONAL,
        BUSINESS,
        PERSONAL_CREDIT_CARD,
        BUSINESS_CREDIT_CARD
    }

    // Enum for Status
    public enum Status {
        ACTIVE,
        PAID,
        BLOCKED // For Credit Card
    }
}
