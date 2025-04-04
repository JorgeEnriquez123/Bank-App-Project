package com.jorge.credits;

import com.jorge.credits.api.CreditsApiDelegate;
import com.jorge.credits.model.*;
import com.jorge.credits.service.CreditService;
import com.jorge.credits.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditApiDelegateImpl implements CreditsApiDelegate {
    private final CreditService creditService;
    private final OperationService operationService;


    @Override
    public Mono<CreditResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, Mono<ConsumptionRequest> consumptionRequest, ServerWebExchange exchange) {
        return consumptionRequest.flatMap(consumptionRequest1 ->
                operationService.consumeCreditCardByCreditCardNumber(creditCardNumber, consumptionRequest1.getAmount()));
    }

    @Override
    public Mono<CreditResponse> createCredit(Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
        return creditRequest.flatMap(creditService::createCredit);
    }

    @Override
    public Mono<Void> deleteCreditById(String id, ServerWebExchange exchange) {
        return creditService.deleteCreditById(id);
    }

    @Override
    public Flux<CreditResponse> getAllCredits(ServerWebExchange exchange) {
        return creditService.getAllCredits();
    }

    @Override
    public Flux<CreditResponse> getCreditsByCustomerDni(String customerDni, ServerWebExchange exchange) {
        return creditService.getCreditsByCustomerDni(customerDni);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String id, ServerWebExchange exchange) {
        return operationService.getTransactionsByCreditId(id);
    }

    @Override
    public Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber, ServerWebExchange exchange) {
        return creditService.getCreditCardAvailableBalanceByCreditCardNumber(creditCardNumber);
    }

    @Override
    public Mono<CreditResponse> payCreditById(String id, Mono<CreditPaymentRequest> creditPaymentRequest, ServerWebExchange exchange) {
        return creditPaymentRequest.flatMap(
                creditPaymentRequest1 ->
                        operationService.payCreditById(id, creditPaymentRequest1));
    }

    @Override
    public Mono<CreditResponse> payCreditCardByCreditCardNumber(String creditCardNumber, Mono<CreditPaymentRequest> creditPaymentRequest, ServerWebExchange exchange) {
        return creditPaymentRequest.flatMap(
                creditPaymentRequest1 ->
                        operationService.payCreditCardByCreditCardNumber(creditCardNumber, creditPaymentRequest1));
    }

    @Override
    public Mono<CreditResponse> updateCreditById(String id, Mono<CreditRequest> creditRequest, ServerWebExchange exchange) {
        return creditRequest.flatMap(
                creditRequest1 -> creditService.updateCreditById(id, creditRequest1));
    }
}
