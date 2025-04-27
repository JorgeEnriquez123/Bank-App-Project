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
public class BootCoinExchangeRateResponse {
    private String id;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDateTime effectiveDate;
    private LocalDateTime createdAt;
}
