package com.jorge.accounts.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootCoinExchangeKafkaMessage {
    private String petitionId;
    private BigDecimal bootCoinAmount;
    private BigDecimal paymentAmount;
    private PaymentType paymentType;
    private String paymentMethodId;         // Can be either a bank account or a yanki wallet
    private PaymentType sellerPaymentType;
    private String sellerPaymentMethodId;   // Can be either a bank account or a yanki wallet
    private String buyerBootCoinWalletId;
    private String sellerBootCoinWalletId;

    public enum PaymentType {
        YANKI_WALLET,
        BANK_ACCOUNT
    }
}