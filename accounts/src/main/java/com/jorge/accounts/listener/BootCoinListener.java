package com.jorge.accounts.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.accounts.listener.dto.AccountNumberAssociationKafkaMessage;
import com.jorge.accounts.listener.dto.BootCoinExchangeKafkaMessage;
import com.jorge.accounts.listener.dto.BootCoinPurchaseKafkaMessage;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinListener {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AccountService accountService;
    private final DebitCardRepository debitCardRepository;

    @KafkaListener(topics = "bootcoin-purchase-request", groupId = "accounts-bootcoin-purchase-group")
    public void listenBootCoinPurchase(String message) {
        try {
            BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            log.info("Received BootCoin purchase message: {}", bootCoinPurchaseKafkaMessage);

            accountService.purchaseBootCoin(bootCoinPurchaseKafkaMessage)
                    .doOnSuccess(response -> {
                        log.info("BootCoin purchase successful: {}", response);
                        sendPurchaseSuccessfulMessage(bootCoinPurchaseKafkaMessage);
                    })
                    .onErrorResume(e -> {
                        log.error("BootCoin purchase failed: {}", e.getMessage(), e);
                        sendPurchaseFailedMessage(bootCoinPurchaseKafkaMessage);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing BootCoin purchase message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-purchase-yanki-validation-success", groupId = "accounts-yanki-bootcoin-yanki-purchase-group")
    public void listenYankiBootCoinPurchase(String message) {
        try {
            BootCoinPurchaseKafkaMessage bootCoinPurchaseKafkaMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            log.info("Received Yanki BootCoin purchase message: {}", bootCoinPurchaseKafkaMessage);
            String debitCardNumber = bootCoinPurchaseKafkaMessage.getPaymentMethodId();

            log.info("Finding debit card for number: {}", debitCardNumber);
            debitCardRepository.findByDebitCardNumber(debitCardNumber)
                    .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found for number: " + debitCardNumber)))
                    .flatMap(debitCard -> {
                        String accountNumber = debitCard.getMainLinkedAccountNumber();
                        bootCoinPurchaseKafkaMessage.setPaymentMethodId(accountNumber);
                        return accountService.purchaseBootCoin(bootCoinPurchaseKafkaMessage);
                    })
                    .doOnSuccess(response -> {
                        log.info("Yanki BootCoin purchase successful: {}", response);
                        sendPurchaseSuccessfulMessage(bootCoinPurchaseKafkaMessage);
                    })
                    .onErrorResume(e -> {
                        log.error("Yanki BootCoin purchase failed: {}", e.getMessage(), e);
                        sendPurchaseFailedMessage(bootCoinPurchaseKafkaMessage);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing Yanki BootCoin purchase message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-exchange-request", groupId = "accounts-bootcoin-exchange-group")
    public void listenBootCoinExchangeRequest(String message) {
        // This one assumes that both buyer and seller accounts have already account numbers to make the transfer with
        try {
            BootCoinExchangeKafkaMessage bootCoinPurchaseKafkaMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);
            log.info("Received BootCoin exchange request message: {}", bootCoinPurchaseKafkaMessage);
            String senderAccountNumber = bootCoinPurchaseKafkaMessage.getPaymentMethodId();
            String receiverAccountNumber = bootCoinPurchaseKafkaMessage.getSellerPaymentMethodId();

            log.info("Creating transfer request from {} to {}", senderAccountNumber, receiverAccountNumber);
            TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setAmount(bootCoinPurchaseKafkaMessage.getPaymentAmount());
                    transferRequest.setReceiverAccountNumber(receiverAccountNumber);
            accountService.transfer(senderAccountNumber, transferRequest)
                    .doOnSuccess(response -> {
                        log.info("BootCoin exchange request successful: {}", response);
                        sendExchangeSuccessfulMessage(bootCoinPurchaseKafkaMessage);
                    })
                    .onErrorResume(e -> {
                        log.error("BootCoin exchange request failed: {}", e.getMessage(), e);
                        sendExchangeFailedMessage(bootCoinPurchaseKafkaMessage);
                        return Mono.empty();
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Error processing BootCoin exchange request message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-exchange-yanki-validation-success", groupId = "accounts-yanki-bootcoin-exchange-group")
    public void listenBootCoinYankiExchangeRequest(String message) {
        try {
            BootCoinExchangeKafkaMessage bootCoinExchangeKafkaMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);
            log.info("Received Yanki BootCoin exchange message: {}", bootCoinExchangeKafkaMessage);

            // BUYER
            Mono<String> buyerAccountNumberMono;
            if (bootCoinExchangeKafkaMessage.getPaymentType() == BootCoinExchangeKafkaMessage.PaymentType.YANKI_WALLET) {
                log.info("Finding debit card for number: {}", bootCoinExchangeKafkaMessage.getPaymentMethodId());
                buyerAccountNumberMono = debitCardRepository.findByDebitCardNumber(bootCoinExchangeKafkaMessage.getPaymentMethodId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found for number: " + bootCoinExchangeKafkaMessage.getPaymentMethodId())))
                        .map(DebitCard::getMainLinkedAccountNumber);
            } else {
                // This assumes that the payment method ID is already an account number
                buyerAccountNumberMono = Mono.just(bootCoinExchangeKafkaMessage.getPaymentMethodId());

            }
            // SELLER
            Mono<String> sellerAccountNumberMono;
            if (bootCoinExchangeKafkaMessage.getSellerPaymentType() == BootCoinExchangeKafkaMessage.PaymentType.YANKI_WALLET) {
                log.info("Finding debit card for number: {}", bootCoinExchangeKafkaMessage.getSellerPaymentMethodId());
                sellerAccountNumberMono = debitCardRepository.findByDebitCardNumber(bootCoinExchangeKafkaMessage.getSellerPaymentMethodId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Debit card not found for number: " + bootCoinExchangeKafkaMessage.getSellerPaymentMethodId())))
                        .map(DebitCard::getMainLinkedAccountNumber);
            } else {
                // This assumes that the payment method ID is already an account number
                sellerAccountNumberMono = Mono.just(bootCoinExchangeKafkaMessage.getSellerPaymentMethodId());
            }

            // COMBINE AND EXECUTE THE TRANSFER
            Mono.zip(buyerAccountNumberMono, sellerAccountNumberMono)
                    .flatMap(tuple -> {
                        String buyerAccountNumber = tuple.getT1();
                        String sellerAccountNumber = tuple.getT2();

                        TransferRequest transferRequest = new TransferRequest();
                        transferRequest.setAmount(bootCoinExchangeKafkaMessage.getPaymentAmount());
                        transferRequest.setReceiverAccountNumber(sellerAccountNumber);

                        return accountService.transfer(buyerAccountNumber, transferRequest)
                                .thenReturn(bootCoinExchangeKafkaMessage);
                    })
                    .doOnSuccess(this::sendExchangeSuccessfulMessage)
                    .onErrorResume(e -> {
                        log.error("Yanki BootCoin exchange request failed: {}", e.getMessage(), e);
                        sendExchangeFailedMessage(bootCoinExchangeKafkaMessage);
                        return Mono.empty();
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("Error processing Yanki BootCoin exchange message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-account-association-request", groupId = "accounts-bootcoin-association-group")
    public void listenBootCoinAccountAssociation(String message) {
        log.info("Received message from Kafka topic 'bootcoin-account-association-request'");
        try {
            AccountNumberAssociationKafkaMessage associationMessage = objectMapper.readValue(message, AccountNumberAssociationKafkaMessage.class);
            accountService.getAccountByAccountNumber(associationMessage.getAccountNumber())
                    .switchIfEmpty(Mono.defer(() -> {
                        log.error("Account not found for number: {}", associationMessage.getAccountNumber());
                        return Mono.fromRunnable(() -> sendAccountAssociationFailedMessage(associationMessage));
                    }))
                    .flatMap(account -> {
                        log.info("Account found: {}", account);
                        return Mono.fromRunnable(() -> sendAccountAssociationSuccessfulMessage(associationMessage));
                    })
                    .onErrorResume(e -> {
                        log.error("Error during account association: {}", e.getMessage(), e);
                        return Mono.fromRunnable(() -> sendAccountAssociationFailedMessage(associationMessage));
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing AccountNumberAssociationKafkaMessage: {}", e.getMessage(), e);
        }
    }

    private void sendAccountAssociationSuccessfulMessage(AccountNumberAssociationKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-account-association-success", successMessage);
            log.info("Successfully sent Account Association Success Message for account: {}", message.getAccountNumber());
        } catch (Exception e) {
            log.error("Error sending Account Association Success message: {}", e.getMessage(), e);
        }
    }

    private void sendAccountAssociationFailedMessage(AccountNumberAssociationKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-account-association-failed", failMessage);
            log.info("Successfully sent Account Association Failed Message for account: {}", message.getAccountNumber());
        } catch (Exception e) {
            log.error("Error sending Account Association Failed message: {}", e.getMessage(), e);
        }
    }

    private void sendPurchaseSuccessfulMessage(BootCoinPurchaseKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-purchase-success", successMessage);
            log.info("Successfully sent BootCoin purchase success message for account: {}", message.getPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin purchase success message: {}", e.getMessage(), e);
        }
    }

    private void sendPurchaseFailedMessage(BootCoinPurchaseKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-purchase-failed", failMessage);
            log.info("Successfully sent BootCoin purchase failed message for account: {}", message.getPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin purchase failed message: {}", e.getMessage(), e);
        }
    }

    private void sendExchangeSuccessfulMessage(BootCoinExchangeKafkaMessage message) {
        try {
            String successMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-exchange-success", successMessage);
            log.info("Successfully sent BootCoin exchange success message for buyer account: {}, and seller account: {}"
                    , message.getPaymentMethodId(), message.getSellerPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin exchange success message: {}", e.getMessage(), e);
        }
    }

    private void sendExchangeFailedMessage(BootCoinExchangeKafkaMessage message) {
        try {
            String failMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bootcoin-exchange-failed", failMessage);
            log.info("Successfully sent BootCoin exchange failed message for buyer account: {}, and seller account: {}"
                    , message.getPaymentMethodId(), message.getSellerPaymentMethodId());
        } catch (Exception e) {
            log.error("Error sending BootCoin exchange failed message: {}", e.getMessage(), e);
        }
    }
}
