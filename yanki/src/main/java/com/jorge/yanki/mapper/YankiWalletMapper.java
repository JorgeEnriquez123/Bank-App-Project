package com.jorge.yanki.mapper;

import com.jorge.yanki.dto.request.YankiWalletRequest;
import com.jorge.yanki.dto.response.YankiWalletResponse;
import com.jorge.yanki.model.YankiWallet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class YankiWalletMapper {
    public YankiWallet mapToYankiWallet(YankiWalletRequest yankiWalletRequest) {
        return YankiWallet.builder()
                .documentNumber(yankiWalletRequest.getDocumentNumber())
                .documentType(YankiWallet.DocumentType.valueOf(yankiWalletRequest.getDocumentType().name()))
                .phoneNumber(yankiWalletRequest.getPhoneNumber())
                .imei(yankiWalletRequest.getImei())
                .email(yankiWalletRequest.getEmail())
                .associatedDebitCardNumber(yankiWalletRequest.getAssociatedDebitCardNumber())
                // Defined values
                .status(yankiWalletRequest.getAssociatedDebitCardNumber() != null
                        ? YankiWallet.YankiWalletStatus.ACTIVE : YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public YankiWalletResponse mapToYankiWalletResponse(YankiWallet yankiWallet) {
        YankiWalletResponse yankiWalletResponse = new YankiWalletResponse();
        yankiWalletResponse.setId(yankiWallet.getId());
        yankiWalletResponse.setDocumentNumber(yankiWallet.getDocumentNumber());
        yankiWalletResponse.setDocumentType(YankiWalletResponse.DocumentType.valueOf(yankiWallet.getDocumentType().name()));
        yankiWalletResponse.setPhoneNumber(yankiWallet.getPhoneNumber());
        yankiWalletResponse.setImei(yankiWallet.getImei());
        yankiWalletResponse.setEmail(yankiWallet.getEmail());
        yankiWalletResponse.setAssociatedDebitCardNumber(yankiWallet.getAssociatedDebitCardNumber());
        yankiWalletResponse.setStatus(YankiWalletResponse.YankiWalletStatus.valueOf(yankiWallet.getStatus().name()));
        yankiWalletResponse.setCreatedAt(yankiWallet.getCreatedAt());
        return yankiWalletResponse;
    }
}
