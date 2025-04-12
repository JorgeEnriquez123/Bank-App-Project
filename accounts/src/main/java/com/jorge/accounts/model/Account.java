package com.jorge.accounts.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public abstract class Account {
    @Id
    private String id;
    private String accountNumber;
    private BigDecimal balance;
    private String customerId;
    private AccountType accountType;
    private LocalDateTime createdAt;

    private Integer movementsThisMonth;
    private Integer maxMovementsFeeFreeThisMonth;
    private Boolean isCommissionFeeActive;
    private BigDecimal movementCommissionFee;

    public enum AccountType{
        SAVINGS, CHECKING, FIXED_TERM
    }
}
