package com.jorge.credits;

import com.jorge.credits.api.CreditCardsApiDelegate;
import com.jorge.credits.model.*;
import com.jorge.credits.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditCardApiDelegateImpl implements CreditCardsApiDelegate {
    private final CreditCardService creditCardService;

    @Override
    public Mono<CreditCardResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, Mono<ConsumptionRequest> consumptionRequest, ServerWebExchange exchange) {
        return consumptionRequest.flatMap(consumptionRequest1 ->
                creditCardService.consumeCreditCardByCreditCardNumber(creditCardNumber, consumptionRequest1));
    }

    @Override
    public Mono<CreditCardResponse> createCreditCard(Mono<CreditCardRequest> creditCardRequest, ServerWebExchange exchange) {
        return creditCardRequest.flatMap(creditCardService::createCreditCard);
    }

    @Override
    public Mono<Void> deleteCreditCardById(String id, ServerWebExchange exchange) {
        return creditCardService.deleteCreditCardById(id);
    }

    @Override
    public Flux<CreditCardResponse> getAllCreditCards(ServerWebExchange exchange) {
        return creditCardService.getAllCreditCards();
    }

    @Override
    public Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber, ServerWebExchange exchange) {
        return creditCardService.getCreditCardAvailableBalanceByCreditCardNumber(creditCardNumber);
    }

    @Override
    public Mono<CreditCardResponse> getCreditCardById(String id, ServerWebExchange exchange) {
        return creditCardService.getCreditCardById(id);
    }

    @Override
    public Flux<CreditCardResponse> getCreditCardsByCardHolderId(String cardHolderId, ServerWebExchange exchange) {
        return creditCardService.getCreditCardsByCardHolderId(cardHolderId);
    }

    @Override
    public Mono<CreditCardResponse> payCreditCardByCreditCardNumber(String creditCardNumber, Mono<CreditPaymentRequest> creditPaymentRequest, ServerWebExchange exchange) {
        return creditPaymentRequest.flatMap(creditPaymentRequest1 ->
                creditCardService.payCreditCardByCreditCardNumber(creditCardNumber, creditPaymentRequest1));
    }

    @Override
    public Mono<CreditCardResponse> updateCreditCardById(String id, Mono<CreditCardRequest> creditCardRequest, ServerWebExchange exchange) {
        return creditCardRequest.flatMap(creditCardRequest1 ->
                creditCardService.updateCreditCardById(id, creditCardRequest1));
    }
}
