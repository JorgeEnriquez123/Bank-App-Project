package com.jorge.bootcoin.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountNumberAssociationKafkaMessage {
    private String bootCoinWalletId;
    private String accountNumber;
}
