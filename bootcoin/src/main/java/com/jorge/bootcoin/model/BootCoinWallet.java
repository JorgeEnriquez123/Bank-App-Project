package com.jorge.bootcoin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "boot-coin-wallets")
public class BootCoinWallet {
    @Id
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

    // If the wallet has either a Yanki Waller or an Account associated, it is considered active
    // Otherwise, it is considered a "PENDING OPERATIONS APPROVAL" status
    public enum BootCoinWalletStatus {
        ACTIVE, BLOCKED, PENDING_OPERATIONS_APPROVAL
    }
}
