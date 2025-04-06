package com.jorge.accounts.mapper;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.SavingsAccount;
import com.jorge.accounts.model.SavingsAccountRequest;
import com.jorge.accounts.model.SavingsAccountResponse;
import com.jorge.accounts.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SavingsAccountMapper {
    private final AccountUtils accountUtils;

    public SavingsAccount mapToSavingsAccount(SavingsAccountRequest savingsAccountRequest) {
        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber(accountUtils.generateAccountNumber()); // Generate AccountNumber
        account.setBalance(savingsAccountRequest.getBalance());
        account.setCustomerDni(savingsAccountRequest.getCustomerDni());
        account.setAccountType(Account.AccountType.SAVINGS); // Set the account type
        account.setCreatedAt(LocalDateTime.now());           // Set the creation timestamp
        account.setMovementsThisMonth(savingsAccountRequest.getMovementsThisMonth());
        account.setMaxMovementsFeeFreeThisMonth(savingsAccountRequest.getMaxMovementsFeeFreeThisMonth());
        account.setIsCommissionFeeActive(savingsAccountRequest.getIsCommissionFeeActive());
        account.setMovementCommissionFee(savingsAccountRequest.getMovementCommissionFee());

        account.setMonthlyMovementsLimit(savingsAccountRequest.getMonthlyMovementsLimit());
        return account;
    }

    public SavingsAccountResponse mapToSavingsAccountResponse(SavingsAccount savingsAccount) {
        SavingsAccountResponse response = new SavingsAccountResponse();
        response.setId(savingsAccount.getId());
        response.setAccountNumber(savingsAccount.getAccountNumber());
        response.setBalance(savingsAccount.getBalance());
        response.setCustomerDni(savingsAccount.getCustomerDni());
        response.setAccountType(SavingsAccountResponse.AccountTypeEnum.valueOf(savingsAccount.getAccountType().name()));
        response.setCreatedAt(savingsAccount.getCreatedAt());
        response.setMovementsThisMonth(savingsAccount.getMovementsThisMonth());
        response.setMaxMovementsFeeFreeThisMonth(savingsAccount.getMaxMovementsFeeFreeThisMonth());
        response.setIsCommissionFeeActive(savingsAccount.getIsCommissionFeeActive());
        response.setMovementCommissionFee(savingsAccount.getMovementCommissionFee());

        response.setMonthlyMovementsLimit(savingsAccount.getMonthlyMovementsLimit());
        return response;
    }
}
