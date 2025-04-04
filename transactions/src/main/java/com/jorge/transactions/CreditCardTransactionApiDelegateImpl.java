package com.jorge.transactions;

import com.jorge.transactions.api.CreditCardTransactionsApiDelegate;
import com.jorge.transactions.model.CreditCardTransactionRequest;
import com.jorge.transactions.model.CreditCardTransactionResponse;
import com.jorge.transactions.service.CreditCardTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreditCardTransactionApiDelegateImpl implements CreditCardTransactionsApiDelegate {
    private final CreditCardTransactionService creditCardTransactionService;

    @Override
    public Flux<CreditCardTransactionResponse> getAllCreditCardTransactions(ServerWebExchange exchange) {
        return creditCardTransactionService.getAllCreditCardTransactions();
    }

    @Override
    public Mono<CreditCardTransactionResponse> createCreditCardTransaction(Mono<CreditCardTransactionRequest> creditCardTransactionRequest, ServerWebExchange exchange) {
        return creditCardTransactionRequest.flatMap(creditCardTransactionService::createCreditCardTransaction);
    }

    @Override
    public Mono<Void> deleteCreditCardTransactionById(String id, ServerWebExchange exchange) {
        return creditCardTransactionService.deleteCreditCardTransactionById(id);
    }

    @Override
    public Mono<CreditCardTransactionResponse> getCreditCardTransactionById(String id, ServerWebExchange exchange) {
        return creditCardTransactionService.getCreditCardTransactionById(id);
    }

    @Override
    public Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber, ServerWebExchange exchange) {
        return creditCardTransactionService.getCreditCardTransactionsByCreditCardNumber(creditCardNumber);
    }

    @Override
    public Mono<CreditCardTransactionResponse> updateCreditCardTransaction(String id, Mono<CreditCardTransactionRequest> creditCardTransactionRequest, ServerWebExchange exchange) {
        return creditCardTransactionRequest.flatMap(creditCardTransactionRequest1 ->
                creditCardTransactionService.updateCreditCardTransaction(id, creditCardTransactionRequest1));
    }
}
