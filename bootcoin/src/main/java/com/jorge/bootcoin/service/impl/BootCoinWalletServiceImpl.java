package com.jorge.bootcoin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.bootcoin.dto.kafka.AccountNumberAssociationKafkaMessage;
import com.jorge.bootcoin.dto.kafka.BootCoinPurchaseKafkaMessage;
import com.jorge.bootcoin.model.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.dto.kafka.YankiWalletAssociationKafkaMessage;
import com.jorge.bootcoin.mapper.BootCoinWalletMapper;
import com.jorge.bootcoin.model.*;
import com.jorge.bootcoin.repository.BootCoinExchangeRateRepository;
import com.jorge.bootcoin.repository.BootCoinWalletRepository;
import com.jorge.bootcoin.service.BootCoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BootCoinWalletServiceImpl implements BootCoinWalletService {
    private final BootCoinWalletRepository bootCoinWalletRepository;
    private final BootCoinExchangeRateRepository bootCoinExchangeRateRepository;
    private final BootCoinWalletMapper bootCoinWalletMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<BootCoinWalletResponse> getAllBootCoinWallets() {
        return bootCoinWalletRepository.findAll()
                .map(bootCoinWalletMapper::mapToBootCoinWalletResponse);
    }

    @Override
    public Mono<BootCoinWalletResponse> getBootCoinWalletById(String id) {
        return bootCoinWalletRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin wallet with id: " + id + " not found")))
                .map(bootCoinWalletMapper::mapToBootCoinWalletResponse);
    }

    @Override
    public Mono<BootCoinWalletResponse> createBootCoinWallet(BootCoinWalletRequest bootCoinWalletRequest) {
        BootCoinWallet bootCoinWallet = bootCoinWalletMapper.mapToBootCoinWallet(bootCoinWalletRequest);
        // Additionally, we can check if the DNI is already registered or not by checking Customer Service.
        // If it is, we would validate the DNI and proceed to create the YankiWallet.
        // If it isn't, we can create a customer by getting their personal information using the DNI (maybe using a third party API).
        // For now, we will just create the YankiWallet assuming the DNI is already registered.
        return bootCoinWalletRepository.save(bootCoinWallet)
                .map(bootCoinWalletMapper::mapToBootCoinWalletResponse);
    }

    @Override
    public Mono<BootCoinWalletResponse> updateBootCoinWallet(String id, BootCoinWalletRequest bootCoinWalletRequest) {
        return bootCoinWalletRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin wallet with id: " + id + " not found")))
                .flatMap(existingWallet -> {
                    BootCoinWallet updatedWallet = bootCoinWalletMapper.mapToBootCoinWallet(bootCoinWalletRequest);
                    updatedWallet.setId(existingWallet.getId());
                    updatedWallet.setCreatedAt(existingWallet.getCreatedAt());
                    return bootCoinWalletRepository.save(updatedWallet);
                })
                .map(bootCoinWalletMapper::mapToBootCoinWalletResponse);
    }

    @Override
    public Mono<Void> deleteBootCoinWalletById(String id) {
        return bootCoinWalletRepository.deleteById(id);
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> purchaseBootCoin(String bootCoinWalletId, BootCoinPurchaseRequest bootCoinPurchaseRequest) {
        // Getting the Exchange Rate from the database
        return bootCoinExchangeRateRepository.findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDateTime.now())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No exchange rate found for the current date")))
                .flatMap(exchangeRate -> {
                    // Bank is selling BootCoin to the user
                    BigDecimal sellRate = exchangeRate.getSellRate();
                    BigDecimal paymentAmountInSoles = bootCoinPurchaseRequest.getBootCoinAmount().multiply(sellRate).setScale(2, RoundingMode.HALF_UP);

                    return bootCoinWalletRepository.findById(bootCoinWalletId)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin wallet with id: " + bootCoinWalletId + " not found")))
                            .flatMap(this::validateBootCoinWalletStatus)
                            .flatMap(buyerBootCoinWallet -> {
                                if (bootCoinPurchaseRequest.getPaymentType() == BootCoinPurchaseRequest.PaymentTypeEnum.BANK_ACCOUNT) {
                                    BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage = createBootCoinPurchaseKafkaMessage(
                                            buyerBootCoinWallet, bootCoinPurchaseRequest, paymentAmountInSoles);
                                    try {
                                        String kafkaMessage = objectMapper.writeValueAsString(bootCoinPurchaseKafkaMessage);
                                        kafkaTemplate.send("bootcoin-purchase-request", kafkaMessage);
                                        SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                                        response.setMessage("Purchase event sent successfully");
                                        return Mono.just(response);
                                    } catch (Exception e) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                                    }
                                }
                                if (bootCoinPurchaseRequest.getPaymentType() == BootCoinPurchaseRequest.PaymentTypeEnum.YANKI_WALLET) {
                                    BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage = createBootCoinPurchaseKafkaMessage(
                                            buyerBootCoinWallet, bootCoinPurchaseRequest, paymentAmountInSoles);
                                    try {
                                        String kafkaMessage = objectMapper.writeValueAsString(bootCoinPurchaseKafkaMessage);
                                        kafkaTemplate.send("bootcoin-purchase-yanki-validation-request", kafkaMessage);
                                        SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                                        response.setMessage("Purchase event sent successfully");
                                        return Mono.just(response);
                                    } catch (Exception e) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                                    }
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment type"));
                                }
                            })
                            .doOnSuccess(response -> log.info("Purchase event sent successfully"))
                            .doOnError(error -> log.error("Error sending purchase event: {}", error.getMessage()));
                });
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> associateAccountNumber(String bootCoinWalletId, AssociateAccountNumberRequest request) {
        log.info("Starting account number association process for bootCoinWalletId: {} and request: {}", bootCoinWalletId, request);
        return getBootCoinWalletById(bootCoinWalletId)
                .flatMap(bootCoinWallet -> {
                    log.info("BootCoin wallet with id: {} found", bootCoinWalletId);
                    AccountNumberAssociationKafkaMessage associationKafkaMessage = new AccountNumberAssociationKafkaMessage();
                    associationKafkaMessage.setBootCoinWalletId(bootCoinWalletId);
                    associationKafkaMessage.setAccountNumber(request.getAccountNumber());

                    try {
                        String kafkaMessage = objectMapper.writeValueAsString(associationKafkaMessage);
                        kafkaTemplate.send("bootcoin-account-association-request", kafkaMessage);
                        SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                        response.setMessage("Account association event sent successfully");
                        return Mono.just(response);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                    }
                })
                .doOnSuccess(response -> log.info("Account association event sent successfully: {}", request))
                .doOnError(error -> log.error("Error sending account association event: {}", error.getMessage()));
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> associateYankiWallet(String bootCoinWalletId, AssociateYankiWalletRequest request) {
        log.info("Starting Yanki wallet association process for bootCoinWalletId: {} and request: {}", bootCoinWalletId, request);
        return getBootCoinWalletById(bootCoinWalletId)
                .flatMap(bootCoinWallet -> {
                    log.info("BootCoin wallet with id: {} found", bootCoinWalletId);
                    YankiWalletAssociationKafkaMessage associationKafkaMessage = new YankiWalletAssociationKafkaMessage();
                    associationKafkaMessage.setBootCoinWalletId(bootCoinWalletId);
                    associationKafkaMessage.setYankiWalletPhoneNumber(request.getYankiPhoneNumber());

                    try {
                        String kafkaMessage = objectMapper.writeValueAsString(associationKafkaMessage);
                        kafkaTemplate.send("bootcoin-yanki-wallet-association-request", kafkaMessage);
                        SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                        response.setMessage("Yanki wallet association event sent successfully");
                        return Mono.just(response);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                    }
                })
                .doOnSuccess(response -> log.info("Yanki wallet association event sent successfully: {}", request))
                .doOnError(error -> log.error("Error sending Yanki wallet association event: {}", error.getMessage()));
    }

    private BootCoinPurchaseKafkaMessage createBootCoinPurchaseKafkaMessage(
            BootCoinWallet bootCoinWallet, BootCoinPurchaseRequest bootCoinPurchaseRequest, BigDecimal paymentAmountInSoles) {
        log.info("Creating BootCoin purchase Kafka message");
        return BootCoinPurchaseKafkaMessage.builder()
                .bootCoinWalletId(bootCoinWallet.getId())
                .paymentMethodId(bootCoinPurchaseRequest.getPaymentMethodId())
                .bootCoinAmount(bootCoinPurchaseRequest.getBootCoinAmount())
                .paymentAmount(paymentAmountInSoles)
                .paymentType(BootCoinPurchaseKafkaMessage.PaymentType
                        .valueOf(bootCoinPurchaseRequest.getPaymentType().name()))
                .build();
    }

    private Mono<BootCoinWallet> validateBootCoinWalletStatus(BootCoinWallet bootCoinWallet) {
        log.info("Checking if BootCoin Wallet is valid for operations");
        if (bootCoinWallet.getStatus() == BootCoinWallet.BootCoinWalletStatus.ACTIVE
                || bootCoinWallet.getStatus() == BootCoinWallet.BootCoinWalletStatus.PENDING_OPERATIONS_APPROVAL) {
            return Mono.just(bootCoinWallet);
        } else {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "BootCoin wallet is not valid for operations"));
        }
    }
}
