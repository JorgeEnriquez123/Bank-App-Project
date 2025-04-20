package com.jorge.yanki.listener.dto;

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
