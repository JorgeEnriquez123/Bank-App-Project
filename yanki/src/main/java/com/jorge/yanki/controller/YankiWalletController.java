package com.jorge.yanki.controller;

import com.jorge.yanki.dto.request.YankiSendPaymentRequest;
import com.jorge.yanki.dto.request.YankiWalletAssociateCardRequest;
import com.jorge.yanki.dto.request.YankiWalletRequest;
import com.jorge.yanki.dto.response.SuccessfulEventOperationResponse;
import com.jorge.yanki.dto.response.YankiWalletResponse;
import com.jorge.yanki.service.YankiWalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/yanki-wallets")
@RequiredArgsConstructor
@Slf4j
public class YankiWalletController {
    private final YankiWalletService yankiWalletService;

    @GetMapping
    public Flowable<YankiWalletResponse> getAllYankiWallets() {
        return yankiWalletService.getAllYankiWallets();
    }

    @GetMapping("/{id}")
    public Single<YankiWalletResponse> getYankiWalletById(@PathVariable String id) {
        return yankiWalletService.getYankiWalletById(id);
    }

    @PostMapping
    public Single<ResponseEntity<YankiWalletResponse>> createYankiWallet(@RequestBody YankiWalletRequest yankiWalletRequest) {
        return yankiWalletService.createYankiWallet(yankiWalletRequest)
                .map(ResponseEntity.status(HttpStatus.CREATED)::body);
    }

    @PutMapping("/{id}")
    public Single<YankiWalletResponse> updateYankiWallet(@PathVariable String id, @RequestBody YankiWalletRequest yankiWalletRequest) {
        return yankiWalletService.updateYankiWallet(id, yankiWalletRequest);
    }

    @DeleteMapping("/{id}")
    public Completable deleteYankiWalletById(@PathVariable String id) {
        return yankiWalletService.deleteYankiWalletById(id);
    }

    @PostMapping("/{id}/associate-debit-card")
    public Single<SuccessfulEventOperationResponse> associateDebitCardToYankiWallet(@PathVariable String id,
                                                                                    @RequestBody YankiWalletAssociateCardRequest yankiWalletAssociateCardRequest) {
        return yankiWalletService.associateDebitCard(id, yankiWalletAssociateCardRequest);
    }

    @PostMapping("/{id}/send-payment")
    public Single<SuccessfulEventOperationResponse> sendPayment(@PathVariable String id,
                                                           @RequestBody YankiSendPaymentRequest yankiSendPaymentRequest) {
        return yankiWalletService.sendPayment(id, yankiSendPaymentRequest);
    }
}
