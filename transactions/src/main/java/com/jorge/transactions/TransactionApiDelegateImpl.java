package com.jorge.transactions;

import com.jorge.transactions.api.TransactionsApiDelegate;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import com.jorge.transactions.service.CreditCardTransactionService;
import com.jorge.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionApiDelegateImpl implements TransactionsApiDelegate {
    private final TransactionService transactionService;

    @Override
    public Mono<TransactionResponse> createTransaction(Mono<TransactionRequest> transactionRequest, ServerWebExchange exchange) {
        return transactionRequest.flatMap(transactionService::createTransaction);
    }

    @Override
    public Mono<Void> deleteTransactionById(String id, ServerWebExchange exchange) {
        return transactionService.deleteTransactionById(id);
    }

    @Override
    public Mono<TransactionResponse> getTransactionById(String id, ServerWebExchange exchange) {
        return transactionService.getTransactionById(id);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByAccountNumber(String accountNumber, ServerWebExchange exchange) {
        return transactionService.getTransactionsByAccountNumber(accountNumber);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId, ServerWebExchange exchange) {
        return transactionService.getTransactionsByCreditId(creditId);
    }

    @Override
    public Mono<TransactionResponse> updateTransaction(String id, Mono<TransactionRequest> transactionRequest, ServerWebExchange exchange) {
        return transactionRequest.flatMap(transactionRequest1 ->
                transactionService.updateTransaction(id, transactionRequest1));
    }
}
