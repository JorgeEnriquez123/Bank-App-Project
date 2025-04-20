package com.jorge.yanki.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YankiSendPaymentRequest {
    private String receiverYankiPhoneNumber;
    private BigDecimal amount;
}
