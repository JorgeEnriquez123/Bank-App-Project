package com.jorge.bootcoin.tempdto;

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
public class BootCoinExchangePetitionResponse {
    private String id;
    private BigDecimal bootCoinAmount;
    private PaymentType paymentType;
    private String paymentMethodId;
    private String buyerBootCoinWalletId;
    private String sellerBootCoinWalletId;
    private LocalDateTime createdAt;
    private Status status;

    public enum PaymentType {
        YANKI_WALLET,
        BANK_ACCOUNT
    }

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
