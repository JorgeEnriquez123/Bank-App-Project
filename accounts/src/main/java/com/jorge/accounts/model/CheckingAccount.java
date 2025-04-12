package com.jorge.accounts.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckingAccount extends Account{
    private BigDecimal maintenanceFee;
    private List<String> holders;
    private List<String> authorizedSigners;

    public CheckingAccount(String id, String accountNumber, BigDecimal balance, String customerId, AccountType accountType, LocalDateTime createdAt, Integer movementsThisMonth, Integer maxMovementsFeeFreeThisMonth, Boolean isCommissionFeeActive, BigDecimal movementCommissionFee, BigDecimal maintenanceFee) {
        super(id, accountNumber, balance, customerId, accountType, createdAt, movementsThisMonth, maxMovementsFeeFreeThisMonth, isCommissionFeeActive, movementCommissionFee);
        this.maintenanceFee = maintenanceFee;
    }
}
