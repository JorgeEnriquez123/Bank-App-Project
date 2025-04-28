package com.jorge.bootcoin.expose;

import com.jorge.bootcoin.api.BootcoinWalletsApiDelegate;
import com.jorge.bootcoin.model.*;
import com.jorge.bootcoin.service.BootCoinWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BootCoinWalletApiDelegateImpl implements BootcoinWalletsApiDelegate {
    private final BootCoinWalletService bootCoinWalletService;

    @Override
    public Mono<SuccessfulEventOperationResponse> associateAccountNumber(String id, Mono<AssociateAccountNumberRequest> associateAccountNumberRequest, ServerWebExchange exchange) {
        return associateAccountNumberRequest.flatMap(request -> bootCoinWalletService.associateAccountNumber(id, request));
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> associateYankiWallet(String id, Mono<AssociateYankiWalletRequest> associateYankiWalletRequest, ServerWebExchange exchange) {
        return associateYankiWalletRequest.flatMap(request -> bootCoinWalletService.associateYankiWallet(id, request));
    }

    @Override
    public Mono<BootCoinWalletResponse> createBootCoinWallet(Mono<BootCoinWalletRequest> bootCoinWalletRequest, ServerWebExchange exchange) {
        return bootCoinWalletRequest.flatMap(bootCoinWalletService::createBootCoinWallet);
    }

    @Override
    public Mono<Void> deleteBootCoinWalletById(String id, ServerWebExchange exchange) {
        return bootCoinWalletService.deleteBootCoinWalletById(id);
    }

    @Override
    public Flux<BootCoinWalletResponse> getAllBootCoinWallets(ServerWebExchange exchange) {
        return bootCoinWalletService.getAllBootCoinWallets();
    }

    @Override
    public Mono<BootCoinWalletResponse> getBootCoinWalletById(String id, ServerWebExchange exchange) {
        return bootCoinWalletService.getBootCoinWalletById(id);
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> purchaseBootCoin(String id, Mono<BootCoinPurchaseRequest> bootCoinPurchaseRequest, ServerWebExchange exchange) {
        return bootCoinPurchaseRequest.flatMap(request -> bootCoinWalletService.purchaseBootCoin(id, request));
    }

    @Override
    public Mono<BootCoinWalletResponse> updateBootCoinWallet(String id, Mono<BootCoinWalletRequest> bootCoinWalletRequest, ServerWebExchange exchange) {
        return bootCoinWalletRequest.flatMap(request -> bootCoinWalletService.updateBootCoinWallet(id, request));
    }
}
