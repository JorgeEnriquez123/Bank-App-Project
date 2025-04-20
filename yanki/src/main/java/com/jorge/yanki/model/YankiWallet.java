package com.jorge.yanki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "yanki_wallets")
public class YankiWallet {
    @Id
    private String id;

    private String documentNumber;
    private DocumentType documentType;

    private String phoneNumber;
    private String imei;

    private String email;

    private String associatedDebitCardNumber;

    // --- Metadatos ---
    private YankiWalletStatus status;
    private LocalDateTime createdAt;

    // --- Enums ---
    public enum DocumentType {
        DNI, CEX, PASSPORT
    }

    public enum YankiWalletStatus {
        ACTIVE, BLOCKED, PENDING_DEBITCARD_ASSOCIATION
    }
}
