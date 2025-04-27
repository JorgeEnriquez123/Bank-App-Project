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
@Document(collection = "boot-coin-transactions")
public class BootCoinTransaction {
    @Id
    private String id;
    private String bootCoinWalletId;
    private BigDecimal amount;
    private String description;

    private BootCoinTransactionType transactionType;
    private LocalDateTime createdAt;

    public enum BootCoinTransactionType {
        DEBIT,
        CREDIT
    }
}
