package com.jorge.credits.webclient.dto.response;

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
    private BigDecimal balance;

    public enum AccountType{
        SAVINGS, CHECKING, FIXED_TERM
    }
}
