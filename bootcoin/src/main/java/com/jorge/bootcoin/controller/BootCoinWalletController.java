package com.jorge.bootcoin.controller;

import com.jorge.bootcoin.dto.kafka.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.service.BootCoinWalletService;
import com.jorge.bootcoin.tempdto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bootcoin-wallets")
@RequiredArgsConstructor
public class BootCoinWalletController {
    private final BootCoinWalletService bootCoinWalletService;

    @GetMapping
    public Flux<BootCoinWalletResponse> getAllBootCoinWallets() {
        return bootCoinWalletService.getAllBootCoinWallets();
    }

    @GetMapping("/{id}")
    public Mono<BootCoinWalletResponse> getBootCoinWalletById(@PathVariable String id) {
        return bootCoinWalletService.getBootCoinWalletById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<BootCoinWalletResponse>> createBootCoinWallet(@RequestBody BootCoinWalletRequest request) {
        return bootCoinWalletService.createBootCoinWallet(request)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @PutMapping("/{id}")
    public Mono<BootCoinWalletResponse> updateBootCoinWallet(@PathVariable String id, @RequestBody BootCoinWalletRequest request) {
        return bootCoinWalletService.updateBootCoinWallet(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteBootCoinWalletById(@PathVariable String id) {
        return bootCoinWalletService.deleteBootCoinWalletById(id);
    }

    @PostMapping("/{id}/purchase")
    public Mono<SuccessfulEventOperationResponse> purchaseBootCoin(@PathVariable String id, @RequestBody BootCoinPurchaseRequest request) {
        return bootCoinWalletService.purchaseBootCoin(id, request);
    }

    @PostMapping("/{id}/associate-account")
    public Mono<SuccessfulEventOperationResponse> associateAccountNumber(@PathVariable String id, @RequestBody AssociateAccountNumberRequest request) {
        return bootCoinWalletService.associateAccountNumber(id, request);
    }

    @PostMapping("/{id}/associate-yanki-wallet")
    public Mono<SuccessfulEventOperationResponse> associateYankiWallet(@PathVariable String id, @RequestBody AssociateYankiWalletRequest request) {
        return bootCoinWalletService.associateYankiWallet(id, request);
    }
}