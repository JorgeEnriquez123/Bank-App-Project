package com.jorge.accounts.listener.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class YankiPaymentKafkaMessage {
    String senderDebitCardNumber;
    String receiverDebitCardNumber;
    BigDecimal amount;
}
