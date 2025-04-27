package com.jorge.bootcoin.tempdto;

import com.jorge.bootcoin.model.BootCoinTransaction;
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
public class BootCoinTransactionResponse {
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
