package com.jorge.accounts.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavingsAccount extends Account {
    private Integer monthlyMovementsLimit;

    public SavingsAccount(String id, String accountNumber, BigDecimal balance, String customerId, AccountType accountType, LocalDateTime createdAt, Integer movementsThisMonth, Integer maxMovementsFeeFreeThisMonth, Boolean isCommissionFeeActive, BigDecimal movementCommissionFee, Integer monthlyMovementsLimit) {
        super(id, accountNumber, balance, customerId, accountType, createdAt, movementsThisMonth, maxMovementsFeeFreeThisMonth, isCommissionFeeActive, movementCommissionFee);
        this.monthlyMovementsLimit = monthlyMovementsLimit;
    }
}
