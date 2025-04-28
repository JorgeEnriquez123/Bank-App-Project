package com.jorge.bootcoin.expose;

import com.jorge.bootcoin.api.BootcoinTransactionsApiDelegate;
import com.jorge.bootcoin.model.BootCoinTransactionRequest;
import com.jorge.bootcoin.model.BootCoinTransactionResponse;
import com.jorge.bootcoin.service.BootCoinTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BootCoinTransactionApiDelegateImpl implements BootcoinTransactionsApiDelegate {
    private final BootCoinTransactionService bootCoinTransactionService;

    @Override
    public Mono<BootCoinTransactionResponse> createBootCoinTransaction(Mono<BootCoinTransactionRequest> bootCoinTransactionRequest, ServerWebExchange exchange) {
        return bootCoinTransactionRequest.flatMap(bootCoinTransactionService::createBootCoinTransaction);
    }

    @Override
    public Mono<Void> deleteBootCoinTransactionById(String id, ServerWebExchange exchange) {
        return bootCoinTransactionService.deleteBootCoinTransactionById(id);
    }

    @Override
    public Flux<BootCoinTransactionResponse> getAllBootCoinTransactions(ServerWebExchange exchange) {
        return bootCoinTransactionService.getAllBootCoinTransactions();
    }

    @Override
    public Mono<BootCoinTransactionResponse> getBootCoinTransactionById(String id, ServerWebExchange exchange) {
        return bootCoinTransactionService.getBootCoinTransactionById(id);
    }

    @Override
    public Mono<BootCoinTransactionResponse> updateBootCoinTransaction(String id, Mono<BootCoinTransactionRequest> bootCoinTransactionRequest, ServerWebExchange exchange) {
        return bootCoinTransactionRequest.flatMap(request -> bootCoinTransactionService.updateBootCoinTransaction(id, request));
    }
}
