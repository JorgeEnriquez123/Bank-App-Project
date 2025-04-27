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
@Document(collection = "boot-coin-exchange-petitions")
public class BootCoinExchangePetition {
    @Id
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
