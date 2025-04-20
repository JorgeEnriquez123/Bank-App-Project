package com.jorge.yanki.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YankiWalletResponse {
    private String id;

    private String documentNumber;
    private YankiWalletResponse.DocumentType documentType;

    private String phoneNumber;
    private String imei;

    private String email;

    private String associatedDebitCardNumber;

    // --- Metadatos ---
    private YankiWalletResponse.YankiWalletStatus status;
    private LocalDateTime createdAt;

    // --- Enums ---
    public enum DocumentType {
        DNI, CEX, PASSPORT
    }

    public enum YankiWalletStatus {
        ACTIVE, BLOCKED, PENDING_DEBITCARD_ASSOCIATION
    }
}
