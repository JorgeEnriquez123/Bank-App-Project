package com.jorge.yanki.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YankiWalletRequest {
    private String documentNumber;
    private YankiWalletRequest.DocumentType documentType;

    private String phoneNumber;
    private String imei;

    private String email;

    private String associatedDebitCardNumber;

    public enum DocumentType {
        DNI, CEX, PASSPORT
    }
}
