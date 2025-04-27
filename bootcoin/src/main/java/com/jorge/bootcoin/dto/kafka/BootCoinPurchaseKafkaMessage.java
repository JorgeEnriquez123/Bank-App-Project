package com.jorge.bootcoin.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootCoinPurchaseKafkaMessage {
    private String bootCoinWalletId;
    private String paymentMethodId;
    private BigDecimal bootCoinAmount;
    private BigDecimal paymentAmount;
    private PaymentType paymentType;

    public enum PaymentType {
        YANKI_WALLET,
        BANK_ACCOUNT
    }
}
