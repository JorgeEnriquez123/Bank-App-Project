package com.jorge.bootcoin.expose;

import com.jorge.bootcoin.api.BootcoinExchangePetitionsApiDelegate;
import com.jorge.bootcoin.model.BootCoinExchangePetitionRequest;
import com.jorge.bootcoin.model.BootCoinExchangePetitionResponse;
import com.jorge.bootcoin.model.BootCoinSellerPaymentMethod;
import com.jorge.bootcoin.model.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.service.BootCoinExchangePetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BootCoinExchangePetitionApiDelegateImpl implements BootcoinExchangePetitionsApiDelegate {
    private final BootCoinExchangePetitionService bootCoinExchangePetitionService;

    @Override
    public Mono<SuccessfulEventOperationResponse> acceptBootCoinExchangePetition(String id, Mono<BootCoinSellerPaymentMethod> bootCoinSellerPaymentMethod, ServerWebExchange exchange) {
        return bootCoinSellerPaymentMethod.flatMap(method -> bootCoinExchangePetitionService.acceptBootCoinExchangePetition(id, method));
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> createPetition(String id, Mono<BootCoinExchangePetitionRequest> bootCoinExchangePetitionRequest, ServerWebExchange exchange) {
        return bootCoinExchangePetitionRequest.flatMap(request -> bootCoinExchangePetitionService.createPetition(id, request));
    }

    @Override
    public Mono<Void> deletePetition(String id, ServerWebExchange exchange) {
        return bootCoinExchangePetitionService.deletePetition(id);
    }

    @Override
    public Flux<BootCoinExchangePetitionResponse> getAllPetitions(ServerWebExchange exchange) {
        return bootCoinExchangePetitionService.getAllPetitions();
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> getPetitionById(String id, ServerWebExchange exchange) {
        return bootCoinExchangePetitionService.getPetitionById(id);
    }

    @Override
    public Mono<BootCoinExchangePetitionResponse> updatePetition(String id, Mono<BootCoinExchangePetitionRequest> bootCoinExchangePetitionRequest, ServerWebExchange exchange) {
        return bootCoinExchangePetitionRequest.flatMap(request -> bootCoinExchangePetitionService.updatePetition(id, request));
    }
}
