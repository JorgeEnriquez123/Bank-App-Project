package com.jorge.yanki.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootCoinYankiWalletAssociationMessage {
    private String bootCoinWalletId;
    private String yankiWalletPhoneNumber;
}
