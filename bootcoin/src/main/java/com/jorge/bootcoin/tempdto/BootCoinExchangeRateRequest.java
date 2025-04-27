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
public class BootCoinExchangeRateRequest {
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDateTime effectiveDate;
}
