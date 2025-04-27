package com.jorge.bootcoin.tempdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssociateYankiWalletRequest {
    private String yankiPhoneNumber;
}
