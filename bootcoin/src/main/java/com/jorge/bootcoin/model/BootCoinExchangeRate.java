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
@Document(collection = "boot-coin-exchange-rates")
public class BootCoinExchangeRate {
    @Id
    private String id;
    private BigDecimal buyRate;     // Rate for when Users exchange soles for BootCoin
    private BigDecimal sellRate;    // Bank sells BootCoin for soles
    private LocalDateTime effectiveDate;
    private LocalDateTime createdAt;
}
