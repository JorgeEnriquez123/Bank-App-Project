package com.jorge.credits.webclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private String accountNumber;
    private AccountType accountType;
    private CurrencyType currencyType;
    private BigDecimal balance;

    public enum AccountType{
        SAVINGS, CHECKING, FIXED_TERM
    }

    public enum CurrencyType{
        PEN, USD
    }
}
