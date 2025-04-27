package com.jorge.bootcoin.mapper;

import com.jorge.bootcoin.model.BootCoinTransaction;
import com.jorge.bootcoin.tempdto.BootCoinTransactionRequest;
import com.jorge.bootcoin.tempdto.BootCoinTransactionResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BootCoinTransactionMapper {
    public BootCoinTransaction mapToBootCoinTransaction(BootCoinTransactionRequest bootCoinTransactionRequest) {
        BootCoinTransaction bootCoinTransaction = new BootCoinTransaction();
        bootCoinTransaction.setBootCoinWalletId(bootCoinTransactionRequest.getBootCoinWalletId());
        bootCoinTransaction.setAmount(bootCoinTransactionRequest.getAmount());
        bootCoinTransaction.setDescription(bootCoinTransactionRequest.getDescription());
        bootCoinTransaction.setTransactionType(BootCoinTransaction.BootCoinTransactionType.valueOf(bootCoinTransactionRequest.getTransactionType().name()));
        bootCoinTransaction.setCreatedAt(LocalDateTime.now());
        return bootCoinTransaction;
    }

    public BootCoinTransactionResponse mapToBootCoinTransactionResponse(BootCoinTransaction bootCoinTransaction) {
        BootCoinTransactionResponse bootCoinTransactionResponse = new BootCoinTransactionResponse();
        bootCoinTransactionResponse.setId(bootCoinTransaction.getId());
        bootCoinTransactionResponse.setBootCoinWalletId(bootCoinTransaction.getBootCoinWalletId());
        bootCoinTransactionResponse.setAmount(bootCoinTransaction.getAmount());
        bootCoinTransactionResponse.setDescription(bootCoinTransaction.getDescription());
        bootCoinTransactionResponse.setTransactionType(BootCoinTransactionResponse.BootCoinTransactionType.valueOf(bootCoinTransaction.getTransactionType().name()));
        bootCoinTransactionResponse.setCreatedAt(bootCoinTransaction.getCreatedAt());
        return bootCoinTransactionResponse;
    }
}
