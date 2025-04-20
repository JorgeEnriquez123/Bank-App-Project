package com.jorge.yanki.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.yanki.dto.request.YankiPaymentKafkaMessage;
import com.jorge.yanki.dto.request.YankiSendPaymentRequest;
import com.jorge.yanki.dto.request.YankiWalletAssociateCardRequest;
import com.jorge.yanki.dto.request.YankiWalletRequest;
import com.jorge.yanki.dto.response.SuccessfulEventOperationResponse;
import com.jorge.yanki.dto.response.YankiWalletResponse;
import com.jorge.yanki.handler.exception.ResourceNotFoundException;
import com.jorge.yanki.handler.exception.YankiWalletNotAvailableForPaymentException;
import com.jorge.yanki.listener.dto.DebitCardAssociationKafkaMessage;
import com.jorge.yanki.mapper.YankiWalletMapper;
import com.jorge.yanki.model.YankiWallet;
import com.jorge.yanki.repository.YankiWalletRepository;
import com.jorge.yanki.service.YankiWalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class YankiWalletServiceImpl implements YankiWalletService {
    private final ObjectMapper objectMapper;
    private final YankiWalletRepository yankiWalletRepository;
    private final YankiWalletMapper yankiWalletMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Flowable<YankiWalletResponse> getAllYankiWallets() {
        return Flowable.fromIterable(yankiWalletRepository.findAll())
                .map(yankiWalletMapper::mapToYankiWalletResponse);
    }

    @Override
    public Single<YankiWalletResponse> getYankiWalletById(String id) {
        return Single.just(yankiWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + id + " not found")))
                .map(yankiWalletMapper::mapToYankiWalletResponse)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<YankiWalletResponse> createYankiWallet(YankiWalletRequest yankiWalletRequest) {
        YankiWallet yankiWallet = yankiWalletMapper.mapToYankiWallet(yankiWalletRequest);
        // Additionally we can check if the DNI is already registered or not by checking CustomerClient.
        // If it is, we just validated the DNI and proceed to create the YankiWallet.
        // If it isn't, we can create a customer by getting their personal information using the DNI (maybe using a third party API).
        // For now, we will just create the YankiWallet assuming the DNI is already registered.
        return Single.just(yankiWalletRepository.save(yankiWallet))
                .doOnSuccess(response -> log.info("YankiWallet created successfully: {}", yankiWalletRequest))
                .doOnError(error -> log.error("Error creating YankiWallet: {}", error.getMessage()))
                .map(yankiWalletMapper::mapToYankiWalletResponse)
                .subscribeOn(Schedulers.io());

    }

    @Override
    public Single<YankiWalletResponse> updateYankiWallet(String id, YankiWalletRequest yankiWalletRequest) {
        return Single.just(yankiWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + id + " not found")))
                .observeOn(Schedulers.io())
                .flatMap(existingYankiWallet -> {
                    YankiWallet updatedYankiWallet = updateYankiWalletFromRequest(existingYankiWallet, yankiWalletRequest);
                    return Single.just(yankiWalletRepository.save(updatedYankiWallet))
                            .doOnSuccess(response ->
                                    log.info("YankiWallet updated successfully: {}", yankiWalletRequest))
                            .doOnError(error -> log.error("Error updating YankiWallet: {}", error.getMessage()))
                            .subscribeOn(Schedulers.io())
                            .map(yankiWalletMapper::mapToYankiWalletResponse);
                });
    }

    @Override
    public Completable deleteYankiWalletById(String id) {
        return Completable.fromAction(() -> yankiWalletRepository.deleteById(id))
                .subscribeOn(Schedulers.io());
    }

    public YankiWallet updateYankiWalletFromRequest(YankiWallet existingYankiWallet, YankiWalletRequest yankiWalletRequest) {
        YankiWallet updatedYankiWallet = yankiWalletMapper.mapToYankiWallet(yankiWalletRequest);
        updatedYankiWallet.setId(existingYankiWallet.getId());
        updatedYankiWallet.setCreatedAt(existingYankiWallet.getCreatedAt());
        return updatedYankiWallet;
    }

    @Override
    public Single<SuccessfulEventOperationResponse> sendPayment(String yankiId, YankiSendPaymentRequest yankiSendPaymentRequest) {
        Single<YankiWallet> yankiWalletSender = Single.just(yankiWalletRepository.findById(yankiId)
                .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + yankiId + " not found")));
        Single<YankiWallet> yankiWalletReceiver = Single.just(yankiWalletRepository.findByPhoneNumber(yankiSendPaymentRequest.getReceiverYankiPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with phone Number: "
                        + yankiSendPaymentRequest.getReceiverYankiPhoneNumber() + " not found")));

        return Single.zip(yankiWalletSender, yankiWalletReceiver, (sender, receiver) -> {
                    validateYankiWallets(sender, receiver);
                    YankiPaymentKafkaMessage yankiPaymentKafkaMessage = new YankiPaymentKafkaMessage();
                    yankiPaymentKafkaMessage.setSenderDebitCardNumber(sender.getAssociatedDebitCardNumber());
                    yankiPaymentKafkaMessage.setReceiverDebitCardNumber(receiver.getAssociatedDebitCardNumber());
                    yankiPaymentKafkaMessage.setAmount(yankiSendPaymentRequest.getAmount());

                    String kafkaMessage = objectMapper.writeValueAsString(yankiPaymentKafkaMessage);
                    kafkaTemplate.send("yanki-payment-request", kafkaMessage);
                    return new SuccessfulEventOperationResponse("Payment event sent successfully");
                }).doOnSuccess(response ->
                        log.info("Payment event sent successfully: {}", yankiSendPaymentRequest))
                .doOnError(error ->
                        log.error("Error sending payment event: {}", error.getMessage()))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<SuccessfulEventOperationResponse> associateDebitCard(String yankiId, YankiWalletAssociateCardRequest yankiWalletAssociateCardRequest) {
        return Single.just(
                        yankiWalletRepository.findById(yankiId)
                                .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + yankiId + " not found")))
                .map(yankiWallet -> {
                    log.info("Yanki wallet with id: {} found", yankiId);
                    DebitCardAssociationKafkaMessage associationKafkaMessage = new DebitCardAssociationKafkaMessage();
                    associationKafkaMessage.setYankiId(yankiId);
                    associationKafkaMessage.setDebitCardNumber(yankiWalletAssociateCardRequest.getDebitCardNumber());

                    String kafkaMessage = objectMapper.writeValueAsString(associationKafkaMessage);
                    kafkaTemplate.send("yanki-debit-card-association-checking-card", kafkaMessage);
                    return new SuccessfulEventOperationResponse("Debit card association event sent successfully");
                })
                .doOnSuccess(response -> log.info("Debit card association event sent successfully: {}", yankiWalletAssociateCardRequest))
                .doOnError(error -> log.error("Error sending debit card association event: {}", error.getMessage()))
                .subscribeOn(Schedulers.io());
    }

    public void validateYankiWallets(YankiWallet yankiWalletSender, YankiWallet yankiWalletReceiver) {
        log.info("Checking if yanki wallets have debit cards associated");
        if(yankiWalletSender.getStatus().equals(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)) {
            throw new YankiWalletNotAvailableForPaymentException("Yanki wallet with id: "
                    + yankiWalletSender.getId() + " is not available for payment. It needs to be associated with a debit card");
        }
        if(yankiWalletReceiver.getStatus().equals(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)) {
            throw new YankiWalletNotAvailableForPaymentException("Yanki wallet with id: "
                    + yankiWalletReceiver.getId() + " is not available for payment. It needs to be associated with a debit card");
        }
    }
}
