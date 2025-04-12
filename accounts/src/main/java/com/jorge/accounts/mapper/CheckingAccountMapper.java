package com.jorge.accounts.mapper;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.CheckingAccount;
import com.jorge.accounts.model.CheckingAccountRequest;
import com.jorge.accounts.model.CheckingAccountResponse;
import com.jorge.accounts.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CheckingAccountMapper {
    private final AccountUtils accountUtils;

    public CheckingAccount mapToCheckingAccount(CheckingAccountRequest checkingAccountRequest) {
        CheckingAccount checkingAccount = new CheckingAccount();
        checkingAccount.setAccountNumber(accountUtils.generateAccountNumber());
        checkingAccount.setBalance(checkingAccountRequest.getBalance());
        checkingAccount.setCustomerId(checkingAccountRequest.getCustomerId());
        checkingAccount.setAccountType(Account.AccountType.CHECKING);
        checkingAccount.setCreatedAt(LocalDateTime.now());
        checkingAccount.setMovementsThisMonth(checkingAccountRequest.getMovementsThisMonth());
        checkingAccount.setMaxMovementsFeeFreeThisMonth(checkingAccountRequest.getMaxMovementsFeeFreeThisMonth());
        checkingAccount.setIsCommissionFeeActive(checkingAccountRequest.getIsCommissionFeeActive());
        checkingAccount.setMovementCommissionFee(checkingAccountRequest.getMovementCommissionFee());

        checkingAccount.setMaintenanceFee(checkingAccountRequest.getMaintenanceFee());
        checkingAccount.setHolders(checkingAccountRequest.getHolders());
        checkingAccount.setAuthorizedSigners(checkingAccountRequest.getAuthorizedSigners());

        return checkingAccount;
    }

    public CheckingAccountResponse mapToCheckingAccountResponse(CheckingAccount checkingAccount) {
        CheckingAccountResponse checkingAccountResponse = new CheckingAccountResponse();
        checkingAccountResponse.setId(checkingAccount.getId());
        checkingAccountResponse.setAccountNumber(checkingAccount.getAccountNumber());
        checkingAccountResponse.setBalance(checkingAccount.getBalance());
        checkingAccountResponse.setCustomerId(checkingAccount.getCustomerId());
        checkingAccountResponse.setAccountType(CheckingAccountResponse.AccountTypeEnum.valueOf(checkingAccount.getAccountType().name()));
        checkingAccountResponse.setCreatedAt(checkingAccount.getCreatedAt());
        checkingAccountResponse.setMovementsThisMonth(checkingAccount.getMovementsThisMonth());
        checkingAccountResponse.setMaxMovementsFeeFreeThisMonth(checkingAccount.getMaxMovementsFeeFreeThisMonth());
        checkingAccountResponse.setIsCommissionFeeActive(checkingAccount.getIsCommissionFeeActive());
        checkingAccountResponse.setMovementCommissionFee(checkingAccount.getMovementCommissionFee());

        checkingAccountResponse.setMaintenanceFee(checkingAccount.getMaintenanceFee());
        checkingAccountResponse.setHolders(checkingAccount.getHolders());
        checkingAccountResponse.setAuthorizedSigners(checkingAccount.getAuthorizedSigners());

        return checkingAccountResponse;
    }
}
