package com.jorge.accounts;

import com.jorge.accounts.api.DebitCardsApiDelegate;
import com.jorge.accounts.model.BalanceResponse;
import com.jorge.accounts.model.DebitCardRequest;
import com.jorge.accounts.model.DebitCardResponse;
import com.jorge.accounts.model.WithdrawalRequest;
import com.jorge.accounts.service.DebitCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DebitCardApiDelegateImpl implements DebitCardsApiDelegate {
    private final DebitCardService debitCardService;

    @Override
    public Mono<DebitCardResponse> createDebitCard(Mono<DebitCardRequest> debitCardRequest, ServerWebExchange exchange) {
        return debitCardRequest.flatMap(debitCardService::createDebitCard);
    }

    @Override
    public Mono<Void> deleteDebitCardByDebitCardNumber(String debitCardNumber, ServerWebExchange exchange) {
        return debitCardService.deleteDebitCardByDebitCardNumber(debitCardNumber);
    }

    @Override
    public Flux<DebitCardResponse> getAllDebitCards(ServerWebExchange exchange) {
        return debitCardService.getAllDebitCards();
    }

    @Override
    public Mono<DebitCardResponse> getDebitCardById(String id, ServerWebExchange exchange) {
        return debitCardService.getDebitCardById(id);
    }

    @Override
    public Mono<DebitCardResponse> updateDebitCardByDebitCardNumber(String debitCardNumber, Mono<DebitCardRequest> debitCardRequest, ServerWebExchange exchange) {
        return debitCardRequest.flatMap(request -> debitCardService.updateDebitCardByDebitCardNumber(debitCardNumber, request));
    }

    @Override
    public Mono<BalanceResponse> withdrawByDebitCardNumber(String debitCardNumber, Mono<WithdrawalRequest> withdrawalRequest, ServerWebExchange exchange) {
        return withdrawalRequest.flatMap(request -> debitCardService.withdrawByDebitCardNumber(debitCardNumber, request));
    }
}
