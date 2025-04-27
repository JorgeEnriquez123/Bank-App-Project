package com.jorge.bootcoin.tempdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootCoinExchangePetitionRequest {
    private BigDecimal bootCoinAmount;
    private PaymentType paymentType;
    private String paymentMethodId;
    private String sellerBootCoinWalletId;

    public enum PaymentType {
        YANKI_WALLET,
        BANK_ACCOUNT
    }
}
