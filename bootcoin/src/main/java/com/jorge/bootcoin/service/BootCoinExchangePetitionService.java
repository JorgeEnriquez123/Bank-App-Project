package com.jorge.bootcoin.service;

import com.jorge.bootcoin.model.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.model.BootCoinExchangePetitionRequest;
import com.jorge.bootcoin.model.BootCoinExchangePetitionResponse;
import com.jorge.bootcoin.model.BootCoinSellerPaymentMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BootCoinExchangePetitionService {
    Flux<BootCoinExchangePetitionResponse> getAllPetitions();
    Mono<BootCoinExchangePetitionResponse> getPetitionById(String id);
    Mono<BootCoinExchangePetitionResponse> createPetition(String buyerBootCoinWalletId, BootCoinExchangePetitionRequest petition);
    Mono<BootCoinExchangePetitionResponse> updatePetition(String id, BootCoinExchangePetitionRequest petition);
    Mono<Void> deletePetition(String id);

    Mono<SuccessfulEventOperationResponse> acceptBootCoinExchangePetition(String petitionId, BootCoinSellerPaymentMethod sellerPaymentMethod);
}
