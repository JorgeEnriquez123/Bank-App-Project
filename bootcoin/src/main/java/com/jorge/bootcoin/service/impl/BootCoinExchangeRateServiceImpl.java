package com.jorge.bootcoin.service.impl;

import com.jorge.bootcoin.mapper.BootCoinExchangeRateMapper;
import com.jorge.bootcoin.repository.BootCoinExchangeRateRepository;
import com.jorge.bootcoin.service.BootCoinExchangeRateService;
import com.jorge.bootcoin.model.BootCoinExchangeRateRequest;
import com.jorge.bootcoin.model.BootCoinExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class BootCoinExchangeRateServiceImpl implements BootCoinExchangeRateService {
    private final BootCoinExchangeRateRepository bootCoinExchangeRateRepository;
    private final BootCoinExchangeRateMapper bootCoinExchangeRateMapper;

    @Override
    public Flux<BootCoinExchangeRateResponse> getAllExchangeRates() {
        return bootCoinExchangeRateRepository.findAll()
                .map(bootCoinExchangeRateMapper::mapToBootCoinExchangeRateResponse);
    }

    @Override
    public Mono<BootCoinExchangeRateResponse> getExchangeRateById(String id) {
        return bootCoinExchangeRateRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange rate with id: " + id + " not found")))
                .map(bootCoinExchangeRateMapper::mapToBootCoinExchangeRateResponse);
    }

    @Override
    public Mono<BootCoinExchangeRateResponse> createExchangeRate(BootCoinExchangeRateRequest exchangeRate) {
        return bootCoinExchangeRateRepository.save(bootCoinExchangeRateMapper.mapToBootCoinExchangeRate(exchangeRate))
                .map(bootCoinExchangeRateMapper::mapToBootCoinExchangeRateResponse);
    }

    @Override
    public Mono<BootCoinExchangeRateResponse> updateExchangeRate(String id, BootCoinExchangeRateRequest exchangeRate) {
        return bootCoinExchangeRateRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange rate with id: " + id + " not found")))
                .flatMap(existingRate -> {
                    var updatedRate = bootCoinExchangeRateMapper.mapToBootCoinExchangeRate(exchangeRate);
                    updatedRate.setId(existingRate.getId());
                    return bootCoinExchangeRateRepository.save(updatedRate);
                })
                .map(bootCoinExchangeRateMapper::mapToBootCoinExchangeRateResponse);
    }

    @Override
    public Mono<Void> deleteExchangeRate(String id) {
        return bootCoinExchangeRateRepository.deleteById(id);
    }
}
