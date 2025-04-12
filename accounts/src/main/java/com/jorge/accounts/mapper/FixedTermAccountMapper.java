package com.jorge.accounts.mapper;

import com.jorge.accounts.model.Account;
import com.jorge.accounts.model.FixedTermAccount;
import com.jorge.accounts.model.FixedTermAccountRequest;
import com.jorge.accounts.model.FixedTermAccountResponse;
import com.jorge.accounts.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FixedTermAccountMapper {
    private final AccountUtils accountUtils;

    public FixedTermAccount mapToFixedTermAccount(FixedTermAccountRequest fixedTermAccountRequest) {
        FixedTermAccount fixedTermAccount = new FixedTermAccount();
        fixedTermAccount.setAccountNumber(accountUtils.generateAccountNumber());
        fixedTermAccount.setBalance(fixedTermAccountRequest.getBalance());
        fixedTermAccount.setCustomerId(fixedTermAccountRequest.getCustomerId());
        fixedTermAccount.setAccountType(Account.AccountType.FIXED_TERM);
        fixedTermAccount.setCreatedAt(LocalDateTime.now());
        fixedTermAccount.setMovementsThisMonth(fixedTermAccountRequest.getMovementsThisMonth());
        fixedTermAccount.setMaxMovementsFeeFreeThisMonth(fixedTermAccountRequest.getMaxMoveementsFeeFreeThisMonth());
        fixedTermAccount.setIsCommissionFeeActive(fixedTermAccountRequest.getIsCommissionFeeActive());
        fixedTermAccount.setMovementCommissionFee(fixedTermAccountRequest.getMovementCommissionFee());

        fixedTermAccount.setAllowedWithdrawal(fixedTermAccountRequest.getAllowedWithdrawal());

        return fixedTermAccount;
    }

    public FixedTermAccountResponse mapToFixedTermAccountResponse(FixedTermAccount fixedTermAccount) {
        FixedTermAccountResponse fixedTermAccountResponse = new FixedTermAccountResponse();
        fixedTermAccountResponse.setId(fixedTermAccount.getId());
        fixedTermAccountResponse.setAccountNumber(fixedTermAccount.getAccountNumber());
        fixedTermAccountResponse.setBalance(fixedTermAccount.getBalance());
        fixedTermAccountResponse.setCustomerId(fixedTermAccount.getCustomerId());
        fixedTermAccountResponse.setAccountType(FixedTermAccountResponse.AccountTypeEnum.valueOf(fixedTermAccount.getAccountType().name()));
        fixedTermAccountResponse.setCreatedAt(fixedTermAccount.getCreatedAt());
        fixedTermAccountResponse.setMovementsThisMonth(fixedTermAccount.getMovementsThisMonth());
        fixedTermAccountResponse.setMaxMovementsFeeFreeThisMonth(fixedTermAccount.getMaxMovementsFeeFreeThisMonth());
        fixedTermAccountResponse.setIsCommissionFeeActive(fixedTermAccount.getIsCommissionFeeActive());
        fixedTermAccountResponse.setMovementCommissionFee(fixedTermAccount.getMovementCommissionFee());

        fixedTermAccountResponse.setAllowedWithdrawal(fixedTermAccount.getAllowedWithdrawal());

        return fixedTermAccountResponse;
    }
}
