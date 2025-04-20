package com.jorge.yanki.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.yanki.dto.request.YankiPaymentKafkaMessage;
import com.jorge.yanki.handler.exception.ResourceNotFoundException;
import com.jorge.yanki.listener.dto.DebitCardAssociationKafkaMessage;
import com.jorge.yanki.model.YankiWallet;
import com.jorge.yanki.repository.YankiWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class YankiWalletListener {
    private final ObjectMapper objectMapper;
    private final YankiWalletRepository yankiWalletRepository;

    @KafkaListener(topics = "yanki-wallet-payment-success", groupId = "yanki-payment-success-group")
    public void listenPaymentSuccessful(String message){
        log.info("Received message from Kafka topic 'yanki-wallet-payment-success': {}", message);
        try {
            YankiPaymentKafkaMessage yankiWallet = objectMapper.readValue(message, YankiPaymentKafkaMessage.class);
            log.info("Payment successful from Yanki Wallet with debit card: {} to Yanki Wallet with debit card: {}"
                    ,yankiWallet.getSenderDebitCardNumber(), yankiWallet.getReceiverDebitCardNumber());
        }
        catch (Exception e) {
            log.error("Error parsing payment success message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "yanki-wallet-payment-failed", groupId = "yanki-payment-failed-group")
    public void listenPaymentFailed(String message){
        log.info("Received message from Kafka topic 'yanki-wallet-payment-failed': {}", message);
        try {
            YankiPaymentKafkaMessage yankiWallet = objectMapper.readValue(message, YankiPaymentKafkaMessage.class);
            log.error("Payment failed from Yanki Wallet with debit card: {} to Yanki Wallet with debit card: {}"
                    ,yankiWallet.getSenderDebitCardNumber(), yankiWallet.getReceiverDebitCardNumber());
        }
        catch (Exception e) {
            log.error("Error parsing payment failed message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "yanki-wallet-debit-card-exists", groupId = "yanki-debit-card-exists-group")
    public void listenDebitCardAssociation(String message) {
        log.info("Received message from Kafka topic 'yanki-wallet-debit-card-exists': {}", message);
        try {
            DebitCardAssociationKafkaMessage debitCardAssociationKafkaMessage =
                    objectMapper.readValue(message, DebitCardAssociationKafkaMessage.class);
            YankiWallet yankiWallet = yankiWalletRepository.findById(debitCardAssociationKafkaMessage.getYankiId())
                    .orElseThrow(() -> new ResourceNotFoundException("Yanki wallet with id: " +
                            debitCardAssociationKafkaMessage.getYankiId() + " not found"));

            yankiWallet.setAssociatedDebitCardNumber(debitCardAssociationKafkaMessage.getDebitCardNumber());
            yankiWallet.setStatus(YankiWallet.YankiWalletStatus.ACTIVE);
            yankiWalletRepository.save(yankiWallet);
            log.info("Debit card number {} successfully associated with yanki wallet id {}",
                    debitCardAssociationKafkaMessage.getDebitCardNumber(), debitCardAssociationKafkaMessage.getYankiId());
        }
        catch (Exception e) {
            log.error("Error parsing debit card association message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "yanki-wallet-debit-card-does-not-exist", groupId = "yanki-debit-card-does-not-exist-group")
    public void listenDebitCardDoesNotExists(String message) {
        log.info("Received message from Kafka topic 'yanki-wallet-debit-card-does-not-exists': {}", message);
        try {
            DebitCardAssociationKafkaMessage debitCardAssociationKafkaMessage =
                    objectMapper.readValue(message, DebitCardAssociationKafkaMessage.class);
            log.error("Debit card number {} not found for yanki wallet id {}",
                    debitCardAssociationKafkaMessage.getDebitCardNumber(), debitCardAssociationKafkaMessage.getYankiId());
        }
        catch (Exception e) {
            log.error("Error parsing debit card not exists message: {}", e.getMessage(), e);
        }
    }
}
