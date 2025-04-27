package com.jorge.yanki.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.yanki.listener.dto.BootCoinExchangeKafkaMessage;
import com.jorge.yanki.listener.dto.BootCoinPurchaseKafkaMessage;
import com.jorge.yanki.listener.dto.BootCoinYankiWalletAssociationMessage;
import com.jorge.yanki.model.YankiWallet;
import com.jorge.yanki.repository.YankiWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinListener {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final YankiWalletRepository yankiWalletRepository;

    @KafkaListener(topics = "bootcoin-purchase-yanki-validation-request", groupId = "yanki-bootcoin-purchase-group")
    public void listenBootCoinYankiPurchase(String message) {
        log.info("Received BootCoin Yanki purchase message: {}", message);
        try {
            BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            log.info("Validating if Yanki wallet with phone Number: {} exists", bootCoinPurchaseKafkaMessage.getPaymentMethodId());

            Optional<YankiWallet> yankiWalletOptional = yankiWalletRepository.findByPhoneNumber(bootCoinPurchaseKafkaMessage.getPaymentMethodId());
            if(yankiWalletOptional.isEmpty()) {
                log.error("Yanki wallet with phone Number: {} does not exist", bootCoinPurchaseKafkaMessage.getPaymentMethodId());
                sendYankiPurchaseValidationFailedMessage(bootCoinPurchaseKafkaMessage);
            }
            else {
                if(yankiWalletOptional.get().getStatus() != YankiWallet.YankiWalletStatus.ACTIVE ||
                    yankiWalletOptional.get().getAssociatedDebitCardNumber() == null) {
                    log.error("Yanki wallet with phone Number: {} is not active or does not have an associated Debit Card", bootCoinPurchaseKafkaMessage.getPaymentMethodId());
                    sendYankiPurchaseValidationFailedMessage(bootCoinPurchaseKafkaMessage);
                    return;
                }
                log.info("Yanki wallet with phone Number: {} exists and it's valid", bootCoinPurchaseKafkaMessage.getPaymentMethodId());
                String debitCardNumber = yankiWalletOptional.get().getAssociatedDebitCardNumber();
                bootCoinPurchaseKafkaMessage.setPaymentMethodId(debitCardNumber);
                sendYankiPurchaseValidationMessage(bootCoinPurchaseKafkaMessage);
            }

        } catch (Exception e) {
            log.error("Error processing BootCoin purchase message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-yanki-wallet-association-request", groupId = "yanki-bootcoin-wallet-association-group")
    public void listenBootCoinYankiWalletAssociation(String message) {
        log.info("Received message from Kafka topic 'bootcoin-yanki-wallet-association-request'");
        try {
            BootCoinYankiWalletAssociationMessage associationMessage = objectMapper.readValue(message, BootCoinYankiWalletAssociationMessage.class);
            Optional<YankiWallet> yankiWalletOptional = yankiWalletRepository.findByPhoneNumber(associationMessage.getYankiWalletPhoneNumber());

            if (yankiWalletOptional.isEmpty()) {
                log.error("Yanki wallet not found for phone number: {}", associationMessage.getYankiWalletPhoneNumber());
                sendYankiWalletAssociationFailedMessage(associationMessage);
            } else {
                YankiWallet yankiWallet = yankiWalletOptional.get();
                log.info("Yanki wallet found: {}", yankiWallet);
                sendYankiWalletAssociationSuccessfulMessage(associationMessage);
            }
        } catch (Exception e) {
            log.error("Error processing BootCoinYankiWalletAssociationMessage: {}", e.getMessage(), e);
        }
    }

    private void sendYankiWalletAssociationSuccessfulMessage(BootCoinYankiWalletAssociationMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-yanki-wallet-association-success", successMessage);
            log.info("Successfully sent Yanki Wallet Association Success Message for phone number: {}", message.getYankiWalletPhoneNumber());
        } catch (Exception e) {
            log.error("Error sending Yanki Wallet Association Success message: {}", e.getMessage(), e);
        }
    }

    private void sendYankiWalletAssociationFailedMessage(BootCoinYankiWalletAssociationMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-yanki-wallet-association-failed", failMessage);
            log.info("Successfully sent Yanki Wallet Association Failed Message for phone number: {}", message.getYankiWalletPhoneNumber());
        } catch (Exception e) {
            log.error("Error sending Yanki Wallet Association Failed message: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "bootcoin-exchange-yanki-validation-request", groupId = "yanki-bootcoin-exchanges-group")
    public void listenBootCoinYankiExchange(String message) {
        log.info("Received BootCoin Yanki exchange message: {}", message);
        try {
            BootCoinExchangeKafkaMessage bootCoinExchangeKafkaMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);

            // Check if the Buyer has a Yanki Wallet
            if (bootCoinExchangeKafkaMessage.getPaymentType() == BootCoinExchangeKafkaMessage.PaymentType.YANKI_WALLET) {
                validateAndSetPaymentMethod(bootCoinExchangeKafkaMessage, bootCoinExchangeKafkaMessage.getPaymentMethodId(), true);
            }

            // Check if the Seller has a Yanki Wallet
            if (bootCoinExchangeKafkaMessage.getSellerPaymentType() == BootCoinExchangeKafkaMessage.PaymentType.YANKI_WALLET) {
                validateAndSetPaymentMethod(bootCoinExchangeKafkaMessage, bootCoinExchangeKafkaMessage.getSellerPaymentMethodId(), false);
            }
            // Both Buyer and Seller have either valid Yanki Wallets with associated Debit Cards, or Account Numbers
            sendYankiExchangeValidationSuccessfulMessage(bootCoinExchangeKafkaMessage);
        } catch (Exception e) {
            log.error("Error processing BootCoin purchase message: {}", e.getMessage(), e);
        }
    }

    private void validateAndSetPaymentMethod(BootCoinExchangeKafkaMessage message, String phoneNumber, boolean isBuyer) {
        Optional<YankiWallet> yankiWalletOptional = yankiWalletRepository.findByPhoneNumber(phoneNumber);
        if (yankiWalletOptional.isEmpty()) {
            log.error("Yanki wallet with phone Number: {} does not exist", phoneNumber);
            sendYankiExchangeValidationFailedMessage(message);
            return;
        }

        YankiWallet yankiWallet = yankiWalletOptional.get();
        if (yankiWallet.getStatus() != YankiWallet.YankiWalletStatus.ACTIVE || yankiWallet.getAssociatedDebitCardNumber() == null) {
            log.error("Yanki wallet with phone Number: {} is not active or does not have an associated Debit Card", phoneNumber);
            sendYankiExchangeValidationFailedMessage(message);
            return;
        }

        log.info("Yanki wallet with phone Number: {} exists and it's valid", phoneNumber);
        String debitCardNumber = yankiWallet.getAssociatedDebitCardNumber();
        if (isBuyer) {
            message.setPaymentMethodId(debitCardNumber);
        } else {
            message.setSellerPaymentMethodId(debitCardNumber);
        }
    }

    private void sendYankiExchangeValidationSuccessfulMessage(BootCoinExchangeKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-exchange-yanki-validation-success", successMessage);
            log.info("Successfully sent BootCoin exchange yanki validation success message for buyer with payment method id: {}, and seller payment method id: {}"
                    ,message.getPaymentMethodId(), message.getSellerPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin exchange success message: {}", e.getMessage(), e);
        }
    }

    private void sendYankiExchangeValidationFailedMessage(BootCoinExchangeKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-exchange-yanki-validation-failed", failMessage);
            log.info("Successfully sent BootCoin exchange yanki validation failed message for buyer with payment method id: {}, and seller payment method id: {}"
                    ,message.getPaymentMethodId(), message.getSellerPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin exchange failed message: {}", e.getMessage(), e);
        }
    }

    private void sendYankiPurchaseValidationMessage(BootCoinPurchaseKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-purchase-yanki-validation-success", successMessage);
            log.info("Successfully sent BootCoin purchase yanki validation success message for account: {}", message.getPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin purchase success message: {}", e.getMessage(), e);
        }
    }

    private void sendYankiPurchaseValidationFailedMessage(BootCoinPurchaseKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-purchase-yanki-validation-failed", failMessage);
            log.info("Successfully sent BootCoin purchase yanki validation failed message for account: {}", message.getPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin purchase failed message: {}", e.getMessage(), e);
        }
    }
}
