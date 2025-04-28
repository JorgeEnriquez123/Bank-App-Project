package com.jorge.bootcoin.mapper;

import com.jorge.bootcoin.model.BootCoinWallet;
import com.jorge.bootcoin.model.BootCoinWalletRequest;
import com.jorge.bootcoin.model.BootCoinWalletResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BootCoinWalletMapper {
    public BootCoinWallet mapToBootCoinWallet(BootCoinWalletRequest bootCoinWalletRequest) {
        BootCoinWallet bootCoinWallet = new BootCoinWallet();
        bootCoinWallet.setDocumentNumber(bootCoinWalletRequest.getDocumentNumber());
        bootCoinWallet.setDocumentType(BootCoinWallet.DocumentType.valueOf(bootCoinWalletRequest.getDocumentType().name()));
        bootCoinWallet.setPhoneNumber(bootCoinWalletRequest.getPhoneNumber());
        bootCoinWallet.setEmail(bootCoinWalletRequest.getEmail());
        bootCoinWallet.setBalance(bootCoinWalletRequest.getBalance());
        bootCoinWallet.setAssociatedYankiWalletId(bootCoinWalletRequest.getAssociatedYankiWalletId());
        bootCoinWallet.setAssociatedAccountNumber(bootCoinWalletRequest.getAssociatedAccountNumber());
        if(bootCoinWalletRequest.getAssociatedAccountNumber() != null || bootCoinWalletRequest.getAssociatedYankiWalletId() != null) {
            bootCoinWallet.setStatus(BootCoinWallet.BootCoinWalletStatus.ACTIVE);
        } else {
            bootCoinWallet.setStatus(BootCoinWallet.BootCoinWalletStatus.PENDING_OPERATIONS_APPROVAL);
        }
        bootCoinWallet.setCreatedAt(LocalDateTime.now());
        return bootCoinWallet;
    }

    public BootCoinWalletResponse mapToBootCoinWalletResponse(BootCoinWallet bootCoinWallet) {
        BootCoinWalletResponse bootCoinWalletResponse = new BootCoinWalletResponse();
        bootCoinWalletResponse.setId(bootCoinWallet.getId());
        bootCoinWalletResponse.setDocumentNumber(bootCoinWallet.getDocumentNumber());
        bootCoinWalletResponse.setDocumentType(BootCoinWalletResponse.DocumentTypeEnum.valueOf(bootCoinWallet.getDocumentType().name()));
        bootCoinWalletResponse.setPhoneNumber(bootCoinWallet.getPhoneNumber());
        bootCoinWalletResponse.setEmail(bootCoinWallet.getEmail());
        bootCoinWalletResponse.setBalance(bootCoinWallet.getBalance());
        bootCoinWalletResponse.setAssociatedYankiWalletId(bootCoinWallet.getAssociatedYankiWalletId());
        bootCoinWalletResponse.setAssociatedAccountNumber(bootCoinWallet.getAssociatedAccountNumber());
        bootCoinWalletResponse.setStatus(BootCoinWalletResponse.StatusEnum.valueOf(bootCoinWallet.getStatus().name()));
        bootCoinWalletResponse.setCreatedAt(bootCoinWallet.getCreatedAt());
        return bootCoinWalletResponse;
    }
}
