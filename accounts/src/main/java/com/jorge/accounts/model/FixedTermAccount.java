package com.jorge.accounts.model;

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
public class FixedTermAccount extends Account{
    private LocalDate allowedWithdrawal;

    public FixedTermAccount(String id, String accountNumber, BigDecimal balance, String customerDni, AccountType accountType, LocalDateTime createdAt, Integer movementsThisMonth, Integer maxMovementsFeeFreeThisMonth, Boolean isCommissionFeeActive, BigDecimal movementCommissionFee, LocalDate allowedWithdrawal) {
        super(id, accountNumber, balance, customerDni, accountType, createdAt, movementsThisMonth, maxMovementsFeeFreeThisMonth, isCommissionFeeActive, movementCommissionFee);
        this.allowedWithdrawal = allowedWithdrawal;
    }
}
