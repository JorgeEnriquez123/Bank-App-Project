package com.jorge.bootcoin.service;

import com.jorge.bootcoin.model.BootCoinExchangeRateRequest;
import com.jorge.bootcoin.model.BootCoinExchangeRateResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BootCoinExchangeRateService {
    Flux<BootCoinExchangeRateResponse> getAllExchangeRates();
    Mono<BootCoinExchangeRateResponse> getExchangeRateById(String id);
    Mono<BootCoinExchangeRateResponse> createExchangeRate(BootCoinExchangeRateRequest exchangeRate);
    Mono<BootCoinExchangeRateResponse> updateExchangeRate(String id, BootCoinExchangeRateRequest exchangeRate);
    Mono<Void> deleteExchangeRate(String id);
}
