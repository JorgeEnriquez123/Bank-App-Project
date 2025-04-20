package com.jorge.accounts.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.accounts.listener.dto.DebitCardAssociationKafkaMessage;
import com.jorge.accounts.listener.dto.YankiPaymentKafkaMessage;
import com.jorge.accounts.model.DebitCard;
import com.jorge.accounts.model.TransferRequest;
import com.jorge.accounts.repository.DebitCardRepository;
import com.jorge.accounts.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebitCardListener {
    private final ObjectMapper objectMapper;
    private final DebitCardRepository debitCardRepository;
    private final AccountService accountService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "yanki-payment-request", groupId = "accounts-yanki-payment-group")
    public void listenYankiPayment(String message) {
        log.info("Received message from Kafka topic 'yanki-payment-request'");
        try {
            YankiPaymentKafkaMessage yankiPaymentMessage = objectMapper.readValue(message, YankiPaymentKafkaMessage.class);
            Mono<DebitCard> senderDebitCardMono = debitCardRepository.findByDebitCardNumber(yankiPaymentMessage.getSenderDebitCardNumber())
                    .switchIfEmpty(Mono.error(new RuntimeException("Sender debit card not found")));
            Mono<DebitCard> receiverDebitCardMono = debitCardRepository.findByDebitCardNumber(yankiPaymentMessage.getReceiverDebitCardNumber())
                    .switchIfEmpty(Mono.error(new RuntimeException("Receiver debit card not found")));

            Mono.zip(senderDebitCardMono, receiverDebitCardMono)
                    .flatMap(tuple -> { // Use flatMap here!
                        DebitCard senderCard = tuple.getT1();
                        DebitCard receiverCard = tuple.getT2();

                        String senderAccountNumber = senderCard.getMainLinkedAccountNumber();
                        String receiverAccountNumber = receiverCard.getMainLinkedAccountNumber();
                        BigDecimal amount = yankiPaymentMessage.getAmount();

                        log.info("Found debit cards. Initiating transfer from account {} to account {} for amount {}",
                                senderAccountNumber, receiverAccountNumber, amount);

                        TransferRequest transferRequest = new TransferRequest();
                        transferRequest.setReceiverAccountNumber(receiverAccountNumber);
                        transferRequest.setAmount(amount);

                        return accountService.transfer(senderAccountNumber, transferRequest)
                                .doOnSuccess(transferResponse -> {
                                    log.info("Transfer successful: {}", transferResponse);
                                    sendPaymentSuccessfulMessage(yankiPaymentMessage);
                                })
                                .onErrorResume(e -> {
                                    log.error("Transfer failed: {}", e.getMessage(), e);
                                    sendPaymentFailedMessage(yankiPaymentMessage);
                                    return Mono.empty();
                                });
                    }).onErrorResume(e -> {
                        log.error("Transfer failed after finding cards: {}", e.getMessage(), e);
                        sendPaymentFailedMessage(yankiPaymentMessage);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error parsing Yanki payment message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "yanki-debit-card-association-checking-card", groupId = "accounts-debit-card-association-group")
    public void listenDebitCardAssociation(String message) {
        log.info("Received message from Kafka topic 'yanki-debit-card-association-checking-card");
        try {
            DebitCardAssociationKafkaMessage debitCardAssociationMessage = objectMapper.readValue(message, DebitCardAssociationKafkaMessage.class);
            debitCardRepository.findByDebitCardNumber(debitCardAssociationMessage.getDebitCardNumber())
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("Debit card not found for number: {}", debitCardAssociationMessage.getDebitCardNumber());
                        return Mono.fromRunnable(() -> sendDebitCardDoesNotExistMessage(debitCardAssociationMessage));
                    }))
                    .flatMap(debitCard -> {
                        log.info("Debit card found: {}", debitCard);
                        return Mono.fromRunnable(() -> sendDebitCardExistsMessage(debitCardAssociationMessage));
                    })
                    .onErrorResume(e -> {
                        log.error("Error finding debit card: {}", e.getMessage(), e);
                        return Mono.fromRunnable(() -> sendDebitCardDoesNotExistMessage(debitCardAssociationMessage));
                    })
                    .subscribe();
        }
        catch (Exception e) {
            log.error("Error parsing debit card association message: {}", e.getMessage(), e);
        }

    }

    private void sendDebitCardExistsMessage(DebitCardAssociationKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("yanki-wallet-debit-card-exists", successMessage);
            log.info("Successfully sent Debit Card Association Success Message for Yanki Wallet with Id: {}", message.getYankiId());

        } catch (Exception e) {
            log.error("Error sending Debit Card Association Success message: {}", e.getMessage(), e);
        }
    }

    private void sendDebitCardDoesNotExistMessage(DebitCardAssociationKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("yanki-wallet-debit-card-does-not-exist", failMessage);
            log.info("Successfully sent Debit Card Association Failed Message for Yanki Wallet with Id: {}", message.getYankiId());

        } catch (Exception e) {
            log.error("Error sending Debit Card Association Failed message: {}", e.getMessage(), e);
        }
    }

    private void sendPaymentSuccessfulMessage(YankiPaymentKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("yanki-wallet-payment-success", successMessage);
            log.info("Successfully sent Payment Success Message for Yanki Wallet sender with Id: {}", message.getSenderDebitCardNumber());

        } catch (Exception e) {
            log.error("Error sending Payment Success message: {}", e.getMessage(), e);
        }
    }

    private void sendPaymentFailedMessage(YankiPaymentKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("yanki-wallet-payment-failed", failMessage);
            log.info("Successfully sent Payment Failed Message for Yanki Wallet sender with Id: {}", message.getSenderDebitCardNumber());

        } catch (Exception e) {
            log.error("Error sending Payment Failed message: {}", e.getMessage(), e);
        }
    }
}
