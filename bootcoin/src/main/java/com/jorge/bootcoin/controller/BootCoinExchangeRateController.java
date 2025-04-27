package com.jorge.bootcoin.controller;

import com.jorge.bootcoin.service.BootCoinExchangeRateService;
import com.jorge.bootcoin.tempdto.BootCoinExchangeRateRequest;
import com.jorge.bootcoin.tempdto.BootCoinExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bootcoin-exchange-rates")
@RequiredArgsConstructor
public class BootCoinExchangeRateController {
    private final BootCoinExchangeRateService bootCoinExchangeRateService;

    @GetMapping
    public Flux<BootCoinExchangeRateResponse> getAllExchangeRates() {
        return bootCoinExchangeRateService.getAllExchangeRates();
    }

    @GetMapping("/{id}")
    public Mono<BootCoinExchangeRateResponse> getExchangeRateById(@PathVariable String id) {
        return bootCoinExchangeRateService.getExchangeRateById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<BootCoinExchangeRateResponse>> createExchangeRate(@RequestBody BootCoinExchangeRateRequest request) {
        return bootCoinExchangeRateService.createExchangeRate(request)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @PutMapping("/{id}")
    public Mono<BootCoinExchangeRateResponse> updateExchangeRate(@PathVariable String id,
                                                                 @RequestBody BootCoinExchangeRateRequest request) {
        return bootCoinExchangeRateService.updateExchangeRate(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteExchangeRate(@PathVariable String id) {
        return bootCoinExchangeRateService.deleteExchangeRate(id);
    }
}
