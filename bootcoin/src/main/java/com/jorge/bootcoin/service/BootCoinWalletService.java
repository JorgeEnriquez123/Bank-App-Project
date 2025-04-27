package com.jorge.bootcoin.service;

import com.jorge.bootcoin.dto.kafka.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.tempdto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BootCoinWalletService {
    Flux<BootCoinWalletResponse> getAllBootCoinWallets();
    Mono<BootCoinWalletResponse> getBootCoinWalletById(String id);
    Mono<BootCoinWalletResponse> createBootCoinWallet(BootCoinWalletRequest bootCoinWalletRequest);
    Mono<BootCoinWalletResponse> updateBootCoinWallet(String id, BootCoinWalletRequest bootCoinWalletRequest);
    Mono<Void> deleteBootCoinWalletById(String id);

    Mono<SuccessfulEventOperationResponse> purchaseBootCoin(String bootCoinWalletId, BootCoinPurchaseRequest bootCoinPurchaseRequest);

    Mono<SuccessfulEventOperationResponse> associateAccountNumber(String bootCoinWalletId, AssociateAccountNumberRequest request);
    Mono<SuccessfulEventOperationResponse> associateYankiWallet(String bootCoinWalletId, AssociateYankiWalletRequest request);

}
