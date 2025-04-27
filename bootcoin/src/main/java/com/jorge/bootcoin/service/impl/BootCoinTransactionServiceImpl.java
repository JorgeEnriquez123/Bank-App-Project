package com.jorge.bootcoin.service.impl;

import com.jorge.bootcoin.mapper.BootCoinTransactionMapper;
import com.jorge.bootcoin.model.BootCoinTransaction;
import com.jorge.bootcoin.repository.BootCoinTransactionRepository;
import com.jorge.bootcoin.service.BootCoinTransactionService;
import com.jorge.bootcoin.tempdto.BootCoinTransactionRequest;
import com.jorge.bootcoin.tempdto.BootCoinTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BootCoinTransactionServiceImpl implements BootCoinTransactionService {
    private final BootCoinTransactionRepository transactionRepository;
    private final BootCoinTransactionMapper bootCoinTransactionMapper;

    @Override
    public Flux<BootCoinTransactionResponse> getAllBootCoinTransactions() {
        return transactionRepository.findAll()
                .map(bootCoinTransactionMapper::mapToBootCoinTransactionResponse);
    }

    @Override
    public Mono<BootCoinTransactionResponse> getBootCoinTransactionById(String id) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin transaction with id: " + id + " not found")))
                .map(bootCoinTransactionMapper::mapToBootCoinTransactionResponse);
    }

    @Override
    public Mono<BootCoinTransactionResponse> createBootCoinTransaction(BootCoinTransactionRequest request) {
        return transactionRepository.save(bootCoinTransactionMapper.mapToBootCoinTransaction(request))
                .map(bootCoinTransactionMapper::mapToBootCoinTransactionResponse);
    }

    @Override
    public Mono<BootCoinTransactionResponse> updateBootCoinTransaction(String id, BootCoinTransactionRequest request) {
        return transactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "BootCoin transaction with id: " + id + " not found")))
                .flatMap(existingTransaction -> {
                    BootCoinTransaction updatedTransaction = bootCoinTransactionMapper.mapToBootCoinTransaction(request);
                    updatedTransaction.setId(existingTransaction.getId());
                    return transactionRepository.save(updatedTransaction);
                })
                .map(bootCoinTransactionMapper::mapToBootCoinTransactionResponse);
    }

    @Override
    public Mono<Void> deleteBootCoinTransactionById(String id) {
        return transactionRepository.deleteById(id);
    }
}
