package com.jorge.bootcoin.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.bootcoin.dto.kafka.AccountNumberAssociationKafkaMessage;
import com.jorge.bootcoin.dto.kafka.BootCoinExchangeKafkaMessage;
import com.jorge.bootcoin.dto.kafka.BootCoinPurchaseKafkaMessage;
import com.jorge.bootcoin.model.BootCoinExchangePetition;
import com.jorge.bootcoin.model.BootCoinTransaction;
import com.jorge.bootcoin.model.BootCoinWallet;
import com.jorge.bootcoin.repository.BootCoinExchangePetitionRepository;
import com.jorge.bootcoin.repository.BootCoinTransactionRepository;
import com.jorge.bootcoin.repository.BootCoinWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountListener {
    private final ObjectMapper objectMapper;
    private final BootCoinWalletRepository bootCoinWalletRepository;
    private final BootCoinTransactionRepository bootCoinTransactionRepository;
    private final BootCoinExchangePetitionRepository bootCoinExchangePetitionRepository;

    @KafkaListener(topics = "bootcoin-purchase-success", groupId = "bootcoin-purchase-success-group")
    public void listenPurchaseSuccess(String message) {
        log.info("Received message from Kafka topic 'bootcoin-purchase-success': {}", message);
        try {
            BootCoinPurchaseKafkaMessage purchaseMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            bootCoinWalletRepository.findById(purchaseMessage.getBootCoinWalletId())
                    .flatMap(wallet -> {
                        wallet.setBalance(wallet.getBalance().add(purchaseMessage.getBootCoinAmount()));
                        return bootCoinWalletRepository.save(wallet);
                    })
                    .flatMap(wallet -> {
                        BootCoinTransaction transaction = BootCoinTransaction.builder()
                                .bootCoinWalletId(purchaseMessage.getBootCoinWalletId())
                                .amount(purchaseMessage.getBootCoinAmount())
                                .description("BootCoin purchase")
                                .transactionType(BootCoinTransaction.BootCoinTransactionType.CREDIT)
                                .createdAt(LocalDateTime.now())
                                .build();
                        return bootCoinTransactionRepository.save(transaction);
                    })
                    .doOnSuccess(transaction -> log.info("BootCoin purchase transaction successfully processed for wallet id: {}", purchaseMessage.getBootCoinWalletId()))
                    .doOnError(e -> log.error("Error processing BootCoin purchase success message: {}", e.getMessage(), e))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error parsing BootCoin purchase success message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-purchase-failed", groupId = "bootcoin-purchase-failed-group")
    public void listenPurchaseFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-purchase-failed': {}", message);
        try {
            BootCoinPurchaseKafkaMessage purchaseMessage = objectMapper.readValue(message, BootCoinPurchaseKafkaMessage.class);
            log.error("BootCoin purchase failed for wallet id: {}", purchaseMessage.getBootCoinWalletId());
        } catch (Exception e) {
            log.error("Error parsing BootCoin purchase failed message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-exchange-success", groupId = "bootcoin-exchange-success-group")
    public void listenExchangeSuccess(String message) {
        log.info("Received message from Kafka topic 'bootcoin-exchange-success': {}", message);
        try {
            BootCoinExchangeKafkaMessage exchangeMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);

            // Fetch the petition using the petition ID and update its status to ACCEPTED
            bootCoinExchangePetitionRepository.findById(exchangeMessage.getPetitionId())
                    .flatMap(petition -> {
                        petition.setStatus(BootCoinExchangePetition.Status.ACCEPTED);
                        return bootCoinExchangePetitionRepository.save(petition);
                    })
                    .doOnSuccess(petition -> log.info("BootCoin exchange petition with ID {} updated to status ACCEPTED", exchangeMessage.getPetitionId()))
                    .doOnError(e -> log.error("Error updating BootCoin exchange petition status: {}", e.getMessage(), e))
                    .subscribe();

            Mono<BootCoinWallet> buyerBootCoinWallet = bootCoinWalletRepository.findById(exchangeMessage.getBuyerBootCoinWalletId())
                    .switchIfEmpty(Mono.error(new RuntimeException("No buyer bootcoin wallet found with id: " + exchangeMessage.getBuyerBootCoinWalletId())));
            Mono<BootCoinWallet> sellerBootCoinWallet = bootCoinWalletRepository.findById(exchangeMessage.getSellerBootCoinWalletId())
                    .switchIfEmpty(Mono.error(new RuntimeException("No seller bootcoin wallet found with id: " + exchangeMessage.getSellerBootCoinWalletId())));

            Mono.zip(buyerBootCoinWallet, sellerBootCoinWallet)
                    .flatMap(tuple -> {
                        BootCoinWallet buyerWallet = tuple.getT1();
                        BootCoinWallet sellerWallet = tuple.getT2();

                        buyerWallet.setBalance(buyerWallet.getBalance().add(exchangeMessage.getBootCoinAmount()));
                        sellerWallet.setBalance(sellerWallet.getBalance().subtract(exchangeMessage.getBootCoinAmount()));

                        return Mono.zip(bootCoinWalletRepository.save(buyerWallet), bootCoinWalletRepository.save(sellerWallet));
                    })
                    .flatMap(tuple -> {
                        // Create a transaction for both BootCoin wallets
                        BootCoinTransaction buyerTransaction = BootCoinTransaction.builder()
                                .bootCoinWalletId(exchangeMessage.getBuyerBootCoinWalletId())
                                .amount(exchangeMessage.getBootCoinAmount())
                                .description("BootCoin buying exchange")
                                .transactionType(BootCoinTransaction.BootCoinTransactionType.CREDIT)
                                .createdAt(LocalDateTime.now())
                                .build();
                        BootCoinTransaction sellerTransaction = BootCoinTransaction.builder()
                                .bootCoinWalletId(exchangeMessage.getSellerBootCoinWalletId())
                                .amount(exchangeMessage.getBootCoinAmount())
                                .description("BootCoin selling exchange")
                                .transactionType(BootCoinTransaction.BootCoinTransactionType.DEBIT)
                                .createdAt(LocalDateTime.now())
                                .build();
                        // Save transactions
                        return Mono.zip(
                                bootCoinTransactionRepository.save(buyerTransaction),
                                bootCoinTransactionRepository.save(sellerTransaction));
                    })
                    .doOnSuccess(wallet ->
                            log.info("BootCoin exchange transaction successfully processed for buyer BootCoin Wallet Id: {}, and seller BootCoin Wallet Id: {}",
                                    exchangeMessage.getBuyerBootCoinWalletId(), exchangeMessage.getSellerBootCoinWalletId()))
                    .doOnError(e -> log.error("Error processing BootCoin exchange success message: {}", e.getMessage(), e))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error parsing BootCoin exchange success message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-exchange-failed", groupId = "bootcoin-exchange-failed-group")
    public void listenExchangeFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-exchange-failed': {}", message);
        try {
            BootCoinExchangeKafkaMessage exchangeMessage = objectMapper.readValue(message, BootCoinExchangeKafkaMessage.class);
            log.error("BootCoin exchange failed for buyer wallet id: {} and seller wallet id: {}",
                    exchangeMessage.getBuyerBootCoinWalletId(), exchangeMessage.getSellerBootCoinWalletId());
        } catch (Exception e) {
            log.error("Error parsing BootCoin exchange failed message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-account-association-success", groupId = "bootcoin-account-association-success-group")
    public void listenAccountNumberAssociationSuccess(String message) {
        log.info("Received message from Kafka topic 'bootcoin-account-association-success': {}", message);
        try {
            AccountNumberAssociationKafkaMessage associationMessage =
                    objectMapper.readValue(message, AccountNumberAssociationKafkaMessage.class);
            bootCoinWalletRepository.findById(associationMessage.getBootCoinWalletId())
                    .flatMap(wallet -> {
                        wallet.setAssociatedAccountNumber(associationMessage.getAccountNumber());
                        wallet.setStatus(BootCoinWallet.BootCoinWalletStatus.ACTIVE);
                        return bootCoinWalletRepository.save(wallet);
                    })
                    .doOnSuccess(wallet -> log.info("Account number {} successfully associated with BootCoin wallet id {}",
                            associationMessage.getAccountNumber(), associationMessage.getBootCoinWalletId()))
                    .doOnError(e -> log.error("Error associating account number with BootCoin wallet: {}", e.getMessage(), e))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error parsing account number association message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "bootcoin-account-association-failed", groupId = "bootcoin-account-association-failed-group")
    public void listenAccountNumberAssociationFailed(String message) {
        log.info("Received message from Kafka topic 'bootcoin-account-association-failed': {}", message);
        try {
            AccountNumberAssociationKafkaMessage associationMessage =
                    objectMapper.readValue(message, AccountNumberAssociationKafkaMessage.class);
            log.error("Account number association failed for BootCoin wallet id: {}", associationMessage.getBootCoinWalletId());
        } catch (Exception e) {
            log.error("Error parsing account number association failed message: {}", e.getMessage(), e);
        }
    }
}
