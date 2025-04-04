package com.jorge.transactions;

import com.jorge.transactions.api.TransactionsApiDelegate;
import com.jorge.transactions.model.TransactionRequest;
import com.jorge.transactions.model.TransactionResponse;
import com.jorge.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionApiDelegateImpl {


    /*@Override
    public Mono<TransactionResponse> createTransaction(Mono<TransactionRequest> transactionRequest, ServerWebExchange exchange) {
        return transactionRequest.flatMap(transactionService::createTransaction);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId, ServerWebExchange exchange) {
        return transactionService.getTransactionsByCreditId(creditId);
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
    public Mono<TransactionResponse> updateTransactionStatusByTransactionId(String id, Mono<UpdateTransactionStatusRequest> updateTransactionStatusRequest, ServerWebExchange exchange) {
        return updateTransactionStatusRequest.flatMap(
                updateTransactionStatusRequest1 ->
                transactionService.updateTransactionById(id, updateTransactionStatusRequest1));
    }*/
}
