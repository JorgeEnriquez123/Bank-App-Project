package com.jorge.bootcoin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.bootcoin.dto.kafka.BootCoinExchangeKafkaMessage;
import com.jorge.bootcoin.model.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.mapper.BootCoinExchangePetitionMapper;
import com.jorge.bootcoin.model.BootCoinExchangePetition;
import com.jorge.bootcoin.model.BootCoinWallet;
import com.jorge.bootcoin.repository.BootCoinExchangePetitionRepository;
import com.jorge.bootcoin.repository.BootCoinExchangeRateRepository;
import com.jorge.bootcoin.repository.BootCoinWalletRepository;
import com.jorge.bootcoin.service.BootCoinExchangePetitionService;
import com.jorge.bootcoin.model.BootCoinExchangePetitionRequest;
import com.jorge.bootcoin.model.BootCoinExchangePetitionResponse;
import com.jorge.bootcoin.model.BootCoinSellerPaymentMethod;
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
public class BootCoinExchangePetitionServiceImpl implements BootCoinExchangePetitionService {
    private final BootCoinExchangePetitionRepository bootCoinExchangePetitionRepository;
    private final BootCoinExchangeRateRepository bootCoinExchangeRateRepository;
    private final BootCoinExchangePetitionMapper bootCoinExchangePetitionMapper;
    private final BootCoinWalletRepository bootCoinWalletRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Flux<BootCoinExchangePetitionResponse> getAllPetitions() {
        log.info("Fetching all BootCoin exchange petitions");
        return bootCoinExchangePetitionRepository.findAll()
                .map(bootCoinExchangePetitionMapper::mapToBootCoinExchangePetitionResponse);
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> getPetitionById(String id) {
        log.info("Fetching BootCoin exchange petition with id: {}", id);
        return bootCoinExchangePetitionRepository.findById(id)
                .switchIfEmpty
                        (Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin exchange petition with id: " + id + " not found")))
                .map(bootCoinExchangePetitionMapper::mapToBootCoinExchangePetitionResponse);
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> createPetition(String buyerBootCoinWalletId, BootCoinExchangePetitionRequest petition) {
        log.info("Creating BootCoin exchange petition");
        if(buyerBootCoinWalletId.equals(petition.getSellerBootCoinWalletId())){
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer and seller BootCoin wallets cannot be the same"));
        }
        return bootCoinWalletRepository.findById(buyerBootCoinWalletId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer BootCoin wallet with id: " + buyerBootCoinWalletId + " not found")))
                .flatMap(buyerWallet -> bootCoinWalletRepository.findById(petition.getSellerBootCoinWalletId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller BootCoin wallet with id: " + petition.getSellerBootCoinWalletId() + " not found")))
                        .flatMap(sellerWallet -> {
                            BootCoinExchangePetition bootCoinExchangePetition = bootCoinExchangePetitionMapper.mapToBootCoinExchangePetition(petition);
                            bootCoinExchangePetition.setBuyerBootCoinWalletId(buyerWallet.getId());
                            bootCoinExchangePetition.setSellerBootCoinWalletId(sellerWallet.getId());
                            return bootCoinExchangePetitionRepository.save(bootCoinExchangePetition);
                        })
                        .map(bootCoinExchangePetitionMapper::mapToBootCoinExchangePetitionResponse)
                );
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> updatePetition(String id, BootCoinExchangePetitionRequest petition) {
        log.info("Updating BootCoin exchange petition with id: {}", id);
        return bootCoinExchangePetitionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin exchange petition with id: " + id + " not found")))
                .flatMap(existingPetition -> {
                    BootCoinExchangePetition updatedPetition = bootCoinExchangePetitionMapper.mapToBootCoinExchangePetition(petition);
                    updatedPetition.setBuyerBootCoinWalletId(existingPetition.getBuyerBootCoinWalletId());
                    updatedPetition.setSellerBootCoinWalletId(petition.getSellerBootCoinWalletId());
                    updatedPetition.setId(existingPetition.getId());
                    updatedPetition.setCreatedAt(existingPetition.getCreatedAt());
                    return bootCoinExchangePetitionRepository.save(updatedPetition);
                })
                .map(bootCoinExchangePetitionMapper::mapToBootCoinExchangePetitionResponse);
    }

    @Override
    public Mono<Void> deletePetition(String id) {
        log.info("Deleting BootCoin exchange petition with id: {}", id);
        return bootCoinExchangePetitionRepository.deleteById(id);
    }

    @Override
    public Mono<SuccessfulEventOperationResponse> acceptBootCoinExchangePetition(String petitionId, BootCoinSellerPaymentMethod sellerPaymentMethod) {
        log.info("Accepting BootCoin exchange petition with id: {}", petitionId);
        return bootCoinExchangePetitionRepository.findById(petitionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange petition not found")))
                .flatMap(petition -> bootCoinExchangeRateRepository.findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDateTime.now())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No exchange rate found for the current date")))
                        .flatMap(exchangeRate -> {
                            BigDecimal buyRate = exchangeRate.getBuyRate();
                            BigDecimal paymentAmount = petition.getBootCoinAmount().multiply(buyRate).setScale(2, RoundingMode.HALF_UP);

                            return bootCoinWalletRepository.findById(petition.getSellerBootCoinWalletId())
                                    .switchIfEmpty(Mono.error(new ResponseStatusException(
                                            HttpStatus.NOT_FOUND, "Seller's BootCoin wallet not found")))
                                    .flatMap(this::validateBootCoinWalletSellingStatus)
                                    .flatMap(sellerWallet -> {
                                        BootCoinExchangeKafkaMessage kafkaMessage = new BootCoinExchangeKafkaMessage();
                                        kafkaMessage.setPetitionId(petitionId);
                                        // BUYER
                                        kafkaMessage.setBootCoinAmount(petition.getBootCoinAmount());
                                        kafkaMessage.setPaymentAmount(paymentAmount);
                                        kafkaMessage.setPaymentType
                                                (BootCoinExchangeKafkaMessage.PaymentType.valueOf(petition.getPaymentType().name()));
                                        kafkaMessage.setPaymentMethodId(petition.getPaymentMethodId());
                                        // SELLER
                                        kafkaMessage.setSellerPaymentType
                                                (BootCoinExchangeKafkaMessage.PaymentType.valueOf(sellerPaymentMethod.getPaymentType().name()));
                                        // COMMON
                                        kafkaMessage.setBuyerBootCoinWalletId(petition.getBuyerBootCoinWalletId());
                                        kafkaMessage.setSellerBootCoinWalletId(sellerWallet.getId());

                                        if (sellerPaymentMethod.getPaymentType() == BootCoinSellerPaymentMethod.PaymentTypeEnum.YANKI_WALLET ||
                                                petition.getPaymentType() == BootCoinExchangePetition.PaymentType.YANKI_WALLET) {
                                            log.info("Yanki Wallet payment method detected");
                                            if (sellerPaymentMethod.getPaymentType() == BootCoinSellerPaymentMethod.PaymentTypeEnum.YANKI_WALLET) {
                                                log.info("Seller payment method is Yanki Wallet");
                                                kafkaMessage.setSellerPaymentMethodId(sellerWallet.getAssociatedYankiWalletId());
                                            } else {
                                                log.info("Seller payment method is Bank Account");
                                                kafkaMessage.setSellerPaymentMethodId(sellerWallet.getAssociatedAccountNumber());
                                            }
                                            try {
                                                String message = objectMapper.writeValueAsString(kafkaMessage);
                                                kafkaTemplate.send("bootcoin-exchange-yanki-validation-request", message);
                                                SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                                                response.setMessage("Exchange acceptance event sent successfully");
                                                return Mono.just(response);
                                            } catch (Exception e) {
                                                return Mono.error(new ResponseStatusException
                                                        (HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                                            }
                                        } else {
                                            log.info("Bank account payment method detected");
                                            try {
                                                kafkaMessage.setSellerPaymentMethodId(sellerWallet.getAssociatedAccountNumber());
                                                String message = objectMapper.writeValueAsString(kafkaMessage);
                                                kafkaTemplate.send("bootcoin-exchange-request", message);
                                                SuccessfulEventOperationResponse response = new SuccessfulEventOperationResponse();
                                                response.setMessage("Exchange acceptance event sent successfully");
                                                return Mono.just(response);
                                            } catch (Exception e) {
                                                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing Kafka message", e));
                                            }
                                        }
                                    });
                        }));
    }

    private Mono<BootCoinWallet> validateBootCoinWalletSellingStatus(BootCoinWallet bootCoinWallet) {
        log.info("Checking if Seller User is valid for operations");
        if (bootCoinWallet.getStatus() == BootCoinWallet.BootCoinWalletStatus.ACTIVE
                || bootCoinWallet.getAssociatedYankiWalletId() != null
                || bootCoinWallet.getAssociatedAccountNumber() != null) {
            return Mono.just(bootCoinWallet);
        } else {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "BootCoin wallet is not valid for operations." +
                    " User is blocked or does not have a Payment Method available"));
        }
    }
}
