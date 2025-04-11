package com.jorge.credits.webclient.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccountResponse {
    private String id;
    private String accountNumber;
    private BigDecimal balance;
    private String customerDni;
    private AccountType accountType;
    private LocalDateTime createdAt;

    private Integer movementsThisMonth;
    private Integer maxMovementsFeeFreeThisMonth;
    private Boolean isCommissionFeeActive;
    private BigDecimal movementCommissionFee;

    private Integer monthlyMovementsLimit;

    private BigDecimal maintenanceFee;
    private List<String> holders;
    private List<String> authorizedSigners;

    private LocalDate allowedWithdrawal;

    public enum AccountType{
        SAVINGS, CHECKING, FIXED_TERM
    }
}
