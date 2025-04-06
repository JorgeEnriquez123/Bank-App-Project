package com.jorge.accounts.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavingsAccount extends Account {
    private Integer monthlyMovementsLimit;

    public SavingsAccount(String id, String accountNumber, BigDecimal balance, String customerDni, AccountType accountType, LocalDateTime createdAt, Integer movementsThisMonth, Integer maxMovementsFeeFreeThisMonth, Boolean isCommissionFeeActive, BigDecimal movementCommissionFee, Integer monthlyMovementsLimit) {
        super(id, accountNumber, balance, customerDni, accountType, createdAt, movementsThisMonth, maxMovementsFeeFreeThisMonth, isCommissionFeeActive, movementCommissionFee);
        this.monthlyMovementsLimit = monthlyMovementsLimit;
    }
}
