package com.jorge.accounts.listener.dto;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DebitCardAssociationKafkaMessage {
    private String yankiId;
    private String debitCardNumber;
}
