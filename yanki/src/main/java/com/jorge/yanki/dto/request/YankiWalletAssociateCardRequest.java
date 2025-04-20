package com.jorge.yanki.dto.request;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class YankiWalletAssociateCardRequest {
    private String debitCardNumber;
}
