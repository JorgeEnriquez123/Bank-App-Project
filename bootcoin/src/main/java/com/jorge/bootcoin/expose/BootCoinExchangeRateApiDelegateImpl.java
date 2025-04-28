package com.jorge.bootcoin.expose;

import com.jorge.bootcoin.api.BootcoinExchangeRatesApiDelegate;
import com.jorge.bootcoin.model.BootCoinExchangeRateRequest;
import com.jorge.bootcoin.model.BootCoinExchangeRateResponse;
import com.jorge.bootcoin.service.BootCoinExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BootCoinExchangeRateApiDelegateImpl implements BootcoinExchangeRatesApiDelegate {
    private final BootCoinExchangeRateService bootCoinExchangeRateService;

    @Override
    public Mono<BootCoinExchangeRateResponse> createExchangeRate(Mono<BootCoinExchangeRateRequest> bootCoinExchangeRateRequest, ServerWebExchange exchange) {
        return bootCoinExchangeRateRequest.flatMap(bootCoinExchangeRateService::createExchangeRate);
    }

    @Override
    public Mono<Void> deleteExchangeRate(String id, ServerWebExchange exchange) {
        return bootCoinExchangeRateService.deleteExchangeRate(id);
    }

    @Override
    public Flux<BootCoinExchangeRateResponse> getAllExchangeRates(ServerWebExchange exchange) {
        return bootCoinExchangeRateService.getAllExchangeRates();
    }

    @Override
    public Mono<BootCoinExchangeRateResponse> getExchangeRateById(String id, ServerWebExchange exchange) {
        return bootCoinExchangeRateService.getExchangeRateById(id);
    }

    @Override
    public Mono<BootCoinExchangeRateResponse> updateExchangeRate(String id, Mono<BootCoinExchangeRateRequest> bootCoinExchangeRateRequest, ServerWebExchange exchange) {
        return bootCoinExchangeRateRequest.flatMap(request -> bootCoinExchangeRateService.updateExchangeRate(id, request));
    }
}
