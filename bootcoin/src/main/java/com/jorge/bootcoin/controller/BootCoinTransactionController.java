package com.jorge.bootcoin.controller;

import com.jorge.bootcoin.dto.kafka.SuccessfulEventOperationResponse;
import com.jorge.bootcoin.service.BootCoinTransactionService;
import com.jorge.bootcoin.tempdto.BootCoinTransactionRequest;
import com.jorge.bootcoin.tempdto.BootCoinTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bootcoin-transactions")
@RequiredArgsConstructor
public class BootCoinTransactionController {
    private final BootCoinTransactionService bootCoinTransactionService;

    @GetMapping
    public Flux<BootCoinTransactionResponse> getAllTransactions() {
        return bootCoinTransactionService.getAllBootCoinTransactions();
    }

    @GetMapping("/{id}")
    public Mono<BootCoinTransactionResponse> getTransactionById(@PathVariable String id) {
        return bootCoinTransactionService.getBootCoinTransactionById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<BootCoinTransactionResponse>> createTransaction(@RequestBody BootCoinTransactionRequest request) {
        return bootCoinTransactionService.createBootCoinTransaction(request)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @PutMapping("/{id}")
    public Mono<BootCoinTransactionResponse> updateTransaction(@PathVariable String id,
                                                               @RequestBody BootCoinTransactionRequest request) {
        return bootCoinTransactionService.updateBootCoinTransaction(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteTransaction(@PathVariable String id) {
        return bootCoinTransactionService.deleteBootCoinTransactionById(id);
    }
}
