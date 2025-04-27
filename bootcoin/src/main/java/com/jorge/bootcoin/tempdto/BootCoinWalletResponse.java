package com.jorge.bootcoin.tempdto;

import com.jorge.bootcoin.model.BootCoinWallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootCoinWalletResponse {
    private String id;
    private String documentNumber;
    private DocumentType documentType;
    private String phoneNumber;
    private String email;
    private BigDecimal balance;
    private String associatedYankiWalletId;
    private String associatedAccountNumber;

    private BootCoinWalletStatus status;
    private LocalDateTime createdAt;

    public enum DocumentType {
        DNI, CEX, PASSPORT
    }

    public enum BootCoinWalletStatus {
        ACTIVE, BLOCKED, PENDING_OPERATIONS_APPROVAL
    }
}
