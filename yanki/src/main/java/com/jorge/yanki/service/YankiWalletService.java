package com.jorge.yanki.service;

import com.jorge.yanki.dto.request.YankiSendPaymentRequest;
import com.jorge.yanki.dto.request.YankiWalletAssociateCardRequest;
import com.jorge.yanki.dto.request.YankiWalletRequest;
import com.jorge.yanki.dto.response.SuccessfulEventOperationResponse;
import com.jorge.yanki.dto.response.YankiWalletResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface YankiWalletService {
    Flowable<YankiWalletResponse> getAllYankiWallets();
    Single<YankiWalletResponse> getYankiWalletById(String id);
    Single<YankiWalletResponse> createYankiWallet(YankiWalletRequest yankiWalletRequest);
    Single<YankiWalletResponse> updateYankiWallet(String id, YankiWalletRequest yankiWalletRequest);
    Completable deleteYankiWalletById(String id);

    Single<SuccessfulEventOperationResponse> sendPayment(String yankiId, YankiSendPaymentRequest yankiSendPaymentRequest);
    Single<SuccessfulEventOperationResponse> associateDebitCard(String yankiId, YankiWalletAssociateCardRequest yankiWalletAssociateCardRequest);
}
