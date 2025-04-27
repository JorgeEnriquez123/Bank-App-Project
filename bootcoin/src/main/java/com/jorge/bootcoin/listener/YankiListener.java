package com.jorge.bootcoin.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.bootcoin.dto.kafka.BootCoinExchangeKafkaMessage;
import com.jorge.bootcoin.dto.kafka.BootCoinPurchaseKafkaMessage;
import com.jorge.bootcoin.dto.kafka.YankiWalletAssociationKafkaMessage;
import com.jorge.bootcoin.model.BootCoinWallet;
import com.jorge.bootcoin.repository.BootCoinWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class YankiListener {
    private final ObjectMapper objectMapper;
    private final BootCoinWalletRepository bootCoinRepository;

    @KafkaListener(topics = "bootcoin-purchase-yanki-validation-failed", groupId = "bootcoin-purchase-yanki-validation-failed-group")
    public void listenPurchaseFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-purchase-yanki-validation-failed': {}", message);
        try {
            BootCoinPurchaseKafkaMessage purchaseMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            log.error("BootCoin purchase failed for wallet id: {} with payment type: {}, and payment method id: {}",
                    purchaseMessage.getBootCoinWalletId(), purchaseMessage.getPaymentType().name() ,purchaseMessage.getPaymentMethodId());
        } catch (Exception e) {
            log.error("Error parsing BootCoin purchase failed message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-exchange-yanki-validation-failed", groupId = "bootcoin-exchange-yanki-validation-failed-group")
    public void listenYankiExchangeValidationFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-exchange-yanki-validation-failed': {}", message);
        try {
            BootCoinExchangeKafkaMessage purchaseMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);
            log.error("BootCoin exchange validation failed for buyer wallet id: {} and seller wallet id: {}",
                    purchaseMessage.getBuyerBootCoinWalletId(), purchaseMessage.getSellerBootCoinWalletId());
        } catch (Exception e) {
            log.error("Error parsing BootCoin exchange validation failed message: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "bootcoin-yanki-wallet-association-success", groupId = "bootcoin-yanki-wallet-association-success-group")
    public void listenYankiWalletAssociationSuccess(String message) {
        log.info("Received message from Kafka topic 'bootcoin-yanki-wallet-association-success': {}", message);
        try {
            YankiWalletAssociationKafkaMessage associationMessage =
                    objectMapper.readValue(message, YankiWalletAssociationKafkaMessage.class);
            bootCoinRepository.findById(associationMessage.getBootCoinWalletId())
                    .flatMap(wallet -> {
                        wallet.setAssociatedYankiWalletId(associationMessage.getYankiWalletPhoneNumber());
                        wallet.setStatus(BootCoinWallet.BootCoinWalletStatus.ACTIVE);
                        return bootCoinRepository.save(wallet);
                    })
                    .doOnSuccess(wallet -> log.info("Yanki wallet phone number {} successfully associated with BootCoin wallet id {}",
                            associationMessage.getYankiWalletPhoneNumber(), associationMessage.getBootCoinWalletId()))
                    .doOnError(e -> log.error("Error associating Yanki wallet phone number with BootCoin wallet: {}", e.getMessage(), e))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error parsing Yanki wallet association message: {}", e.getMessage(), e);
        }
    }
    @KafkaListener(topics = "bootcoin-yanki-wallet-association-failed", groupId = "bootcoin-yanki-wallet-association-failed-group")
    public void listenYankiWalletAssociationFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-yanki-wallet-association-failed': {}", message);
        try {
            YankiWalletAssociationKafkaMessage associationMessage =
                    objectMapper.readValue(message, YankiWalletAssociationKafkaMessage.class);
            log.error("Failed to associate Yanki wallet phone number {} with BootCoin wallet id {}",
                    associationMessage.getYankiWalletPhoneNumber(), associationMessage.getBootCoinWalletId());
        } catch (Exception e) {
            log.error("Error parsing BootCoin Yanki wallet association failed message: {}", e.getMessage(), e);
        }
    }

}
