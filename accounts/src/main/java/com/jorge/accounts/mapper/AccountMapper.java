package com.jorge.accounts.mapper;

import com.jorge.accounts.model.*;
import com.jorge.accounts.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountMapper {
    public AccountResponse mapToAccountResponse(Account account) {
        AccountResponse accountResponse = new AccountResponse();

        accountResponse.setId(account.getId());
        accountResponse.setAccountNumber(account.getAccountNumber());
        accountResponse.setBalance(account.getBalance());
        accountResponse.setCustomerDni(account.getCustomerDni());
        accountResponse.setAccountType(AccountResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
        accountResponse.setCreatedAt(account.getCreatedAt());
        accountResponse.setMovementsThisMonth(account.getMovementsThisMonth());
        accountResponse.setMaxMovementsFeeFreeThisMonth(account.getMaxMovementsFeeFreeThisMonth());
        accountResponse.setIsCommissionFeeActive(account.getIsCommissionFeeActive());
        accountResponse.setMovementCommissionFee(account.getMovementCommissionFee());

        if (account instanceof SavingsAccount savingsAccount) {
            accountResponse.setMonthlyMovementsLimit(savingsAccount.getMonthlyMovementsLimit());
        } else if (account instanceof CheckingAccount checkingAccount) {
            accountResponse.setMaintenanceFee(checkingAccount.getMaintenanceFee());
            accountResponse.setHolders(checkingAccount.getHolders());
            accountResponse.setAuthorizedSigners(checkingAccount.getAuthorizedSigners());
        } else if (account instanceof FixedTermAccount fixedTermAccount) {
            accountResponse.setAllowedWithdrawal(fixedTermAccount.getAllowedWithdrawal());
        }

        return accountResponse;
    }
}
