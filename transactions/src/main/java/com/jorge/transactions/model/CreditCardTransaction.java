package com.jorge.transactions.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_card_transactions")
public class CreditCardTransaction {
    @Id
    private String id;
    private String creditCardNumber;

    private CreditCardTransactionType transactionType;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public enum CreditCardTransactionType {
        CREDIT_CARD_CONSUMPTION,
        CREDIT_CARD_PAYMENT,
    }
}
