package com.jorge.transactions.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String accountNumber;
    //Credit field
    private String creditId;

    private BigDecimal fee;
    private TransactionType transactionType;
    private CurrencyType currencyType;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private LocalDateTime transactionDate;

    public enum TransactionType {
        DEBIT,
        CREDIT,
        DEPOSIT,
        WITHDRAWAL,
        CREDIT_PAYMENT,
        CREDIT_DEPOSIT,
        CREDIT_CARD_CONSUMPTION,
        CREDIT_CARD_PAYMENT,
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
