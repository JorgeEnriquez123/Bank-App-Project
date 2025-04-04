package com.jorge.accounts.mapper;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.AccountRequest;
import com.jorge.accounts.model.AccountResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountMapper {
    public Account mapToAccount(AccountRequest accountRequest) {
        Account account = new Account();
        account.setAccountType(Account.AccountType.valueOf(accountRequest.getAccountType().name()));
        account.setCurrencyType(Account.CurrencyType.valueOf(accountRequest.getCurrencyType().name()));
        account.setBalance(accountRequest.getBalance());
        account.setStatus(Account.AccountStatus.valueOf(accountRequest.getStatus().name()));
        account.setCustomerDni(accountRequest.getCustomerDni());
        account.setMovementsThisMonth(accountRequest.getMovementsThisMonth());
        account.setMaxMovementsFeeFreeThisMonth(accountRequest.getMaxMovementsFeeFreeThisMonth());
        account.setIsCommissionFeeActive(accountRequest.getIsCommissionFeeActive());
        account.setMovementCommissionFee(accountRequest.getMovementCommissionFee());

        switch (accountRequest.getAccountType()) {
            case SAVINGS:
                account.setMonthlyMovementsLimit(accountRequest.getMonthlyMovementsLimit());
                break;
            case CHECKING:
                account.setMaintenanceFee(accountRequest.getMaintenanceFee());
                account.setHolders(accountRequest.getHolders());
                account.setAuthorizedSigners(accountRequest.getAuthorizedSigners());
                break;
            case FIXED_TERM:
                account.setAllowedWithdrawal(accountRequest.getAllowedWithdrawal());
                break;
            default:
                throw new IllegalArgumentException("Unsupported account type: " + accountRequest.getAccountType());
        }
        return account;
    }

    public AccountResponse mapToAccountResponse(Account account) {
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setAccountNumber(account.getAccountNumber());
        accountResponse.setAccountType(AccountResponse.AccountTypeEnum.valueOf(account.getAccountType().name()));
        accountResponse.setCurrencyType(AccountResponse.CurrencyTypeEnum.valueOf(account.getCurrencyType().name()));
        accountResponse.setBalance(account.getBalance());
        accountResponse.setStatus(AccountResponse.StatusEnum.valueOf(account.getStatus().name()));
        accountResponse.setCreatedAt(account.getCreatedAt());
        accountResponse.setCustomerDni(account.getCustomerDni());
        accountResponse.setMovementsThisMonth(account.getMovementsThisMonth());
        accountResponse.setMaxMovementsFeeFreeThisMonth(account.getMaxMovementsFeeFreeThisMonth());
        accountResponse.setIsCommissionFeeActive(account.getIsCommissionFeeActive());
        accountResponse.setMovementCommissionFee(account.getMovementCommissionFee());
        accountResponse.setMonthlyMovementsLimit(account.getMonthlyMovementsLimit());
        accountResponse.setMaintenanceFee(account.getMaintenanceFee());
        accountResponse.setHolders(account.getHolders());
        accountResponse.setAuthorizedSigners(account.getAuthorizedSigners());
        accountResponse.setAllowedWithdrawal(account.getAllowedWithdrawal());
        return accountResponse;
    }
}
