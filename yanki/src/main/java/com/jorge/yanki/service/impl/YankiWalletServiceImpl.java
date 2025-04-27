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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class YankiWalletServiceImpl implements YankiWalletService {
    private final ObjectMapper objectMapper;
    private final YankiWalletRepository yankiWalletRepository;
    private final YankiWalletMapper yankiWalletMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "yankiwallet::";
    private static final long CACHE_TTL_MINUTES = 5;

    @Override
    public Flowable<YankiWalletResponse> getAllYankiWallets() {
        log.info("Fetching all yanki wallets");
        return Flowable.fromIterable(yankiWalletRepository.findAll())
                .map(yankiWalletMapper::mapToYankiWalletResponse);
    }

    @Override
    public Single<YankiWalletResponse> getYankiWalletById(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        return Single.defer(() -> {
            try {
                Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
                if (cachedValue != null) {
                    log.info("Cache HIT for key: {}", cacheKey);
                    YankiWalletResponse response = objectMapper.convertValue(cachedValue, YankiWalletResponse.class);
                    return Single.just(response);
                } else {
                    log.info("Cache MISS for key: {}", cacheKey);
                    YankiWallet yankiWallet = yankiWalletRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + id + " not found"));
                    YankiWalletResponse responseFromDb = yankiWalletMapper.mapToYankiWalletResponse(yankiWallet);
                    try {
                        redisTemplate.opsForValue().set(cacheKey, responseFromDb, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                        log.info("Stored in cache key: {}", cacheKey);
                    } catch (Exception e) {
                        log.error("Error storing value in Redis for key {}: {}", cacheKey, e.getMessage());
                    }
                    return Single.just(responseFromDb);
                }
            } catch (Exception e) {
                return Single.error(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<YankiWalletResponse> createYankiWallet(YankiWalletRequest yankiWalletRequest) {
        log.info("Creating yanki wallet: {}", yankiWalletRequest);
        YankiWallet yankiWallet = yankiWalletMapper.mapToYankiWallet(yankiWalletRequest);
        // Additionally, we can check if the DNI is already registered or not by checking Customer Service.
        // If it is, we would validate the DNI and proceed to create the YankiWallet.
        // If it isn't, we can create a customer by getting their personal information using the DNI (maybe using a third party API).
        // For now, we will just create the BootCoin Wallet assuming the DNI is already registered.
        return Single.just(yankiWalletRepository.save(yankiWallet))
                .doOnSuccess(response -> log.info("YankiWallet created successfully: {}", yankiWalletRequest))
                .doOnError(error -> log.error("Error creating YankiWallet: {}", error.getMessage()))
                .map(yankiWalletMapper::mapToYankiWalletResponse)
                .subscribeOn(Schedulers.io());

    }

    @Override
    public Single<YankiWalletResponse> updateYankiWallet(String id, YankiWalletRequest yankiWalletRequest) {
        log.info("Updating yanki wallet with id: {} and request: {}", id, yankiWalletRequest);
        String cacheKey = CACHE_KEY_PREFIX + id;
        return Single.just(yankiWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " + id + " not found")))
                .observeOn(Schedulers.io())
                .flatMap(existingYankiWallet -> {
                    YankiWallet updatedYankiWallet = updateYankiWalletFromRequest(existingYankiWallet, yankiWalletRequest);
                    return Single.just(yankiWalletRepository.save(updatedYankiWallet))
                            .doOnSuccess(response -> {
                                log.info("YankiWallet updated successfully: {}", yankiWalletRequest);
                                YankiWalletResponse responseFromDb = yankiWalletMapper.mapToYankiWalletResponse(response);
                                try {
                                    redisTemplate.opsForValue().set(cacheKey, responseFromDb, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                                    log.info("Updated cache for key: {}", cacheKey);
                                } catch (Exception e) {
                                    log.error("Error updating cache for key {}: {}", cacheKey, e.getMessage());
                                }
                            })
                            .doOnError(error -> log.error("Error updating YankiWallet: {}", error.getMessage()))
                            .subscribeOn(Schedulers.io())
                            .map(yankiWalletMapper::mapToYankiWalletResponse);
                });
    }

    @Override
    public Completable deleteYankiWalletById(String id) {
        log.info("Deleting yanki wallet with id: {}", id);
        String cacheKey = CACHE_KEY_PREFIX + id;
        return Completable.fromAction(() -> {
            yankiWalletRepository.deleteById(id);
            try {
                redisTemplate.delete(cacheKey);
                log.info("Deleted cache for key: {}", cacheKey);
            } catch (Exception e) {
                log.error("Error deleting cache for key {}: {}", cacheKey, e.getMessage());
            }
        }).subscribeOn(Schedulers.io());
    }

    public YankiWallet updateYankiWalletFromRequest(YankiWallet existingYankiWallet, YankiWalletRequest yankiWalletRequest) {
        YankiWallet updatedYankiWallet = yankiWalletMapper.mapToYankiWallet(yankiWalletRequest);
        updatedYankiWallet.setId(existingYankiWallet.getId());
        updatedYankiWallet.setCreatedAt(existingYankiWallet.getCreatedAt());
        return updatedYankiWallet;
    }

    @Override
    public Single<SuccessfulEventOperationResponse> sendPayment(String yankiId, YankiSendPaymentRequest yankiSendPaymentRequest) {
        log.info("Starting payment process for yankiId: {} and request: {}", yankiId, yankiSendPaymentRequest);
        Single<YankiWalletResponse> yankiWalletSender = getYankiWalletById(yankiId);
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
        log.info("Starting debit card association process for yankiId: {} and request: {}", yankiId, yankiWalletAssociateCardRequest);
        return getYankiWalletById(yankiId)
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

    public void validateYankiWallets(YankiWalletResponse yankiWalletSender, YankiWallet yankiWalletReceiver) {
        log.info("Checking if yanki wallets have debit cards associated");
        if(yankiWalletSender.getStatus().equals(YankiWalletResponse.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)) {
            throw new YankiWalletNotAvailableForPaymentException("Yanki wallet with id: "
                    + yankiWalletSender.getId() + " is not available for payment. It needs to be associated with a debit card");
        }
        if(yankiWalletReceiver.getStatus().equals(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)) {
            throw new YankiWalletNotAvailableForPaymentException("Yanki wallet with id: "
                    + yankiWalletReceiver.getId() + " is not available for payment. It needs to be associated with a debit card");
        }
    }
}
