package com.jorge.transactions.service.impl;

import com.jorge.transactions.mapper.CreditCardTransactionMapper;
import com.jorge.transactions.model.CreditCardTransaction;
import com.jorge.transactions.model.CreditCardTransactionRequest;
import com.jorge.transactions.model.CreditCardTransactionResponse;
import com.jorge.transactions.repository.CreditCardTransactionRepository;
import com.jorge.transactions.service.CreditCardTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardTransactionServiceImpl implements CreditCardTransactionService {
    private final CreditCardTransactionRepository creditCardTransactionRepository;
    private final CreditCardTransactionMapper creditCardTransactionMapper;

    @Override
    public Flux<CreditCardTransactionResponse> getAllCreditCardTransactions() {
        return creditCardTransactionRepository.findAll()
                .map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    @Override
    public Mono<CreditCardTransactionResponse> createCreditCardTransaction(CreditCardTransactionRequest creditCardTransactionRequest) {
        log.info("Creating a new credit card transaction");
        CreditCardTransaction transaction = creditCardTransactionMapper.mapToCreditCardTransaction(creditCardTransactionRequest);
        return creditCardTransactionRepository.save(transaction)
                .map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    @Override
    public Mono<CreditCardTransactionResponse> getCreditCardTransactionById(String id) {
        log.info("Fetching credit card transaction by id: {}", id);
        return creditCardTransactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card transaction with id: " + id + " not found")))
                .map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    @Override
    public Mono<CreditCardTransactionResponse> updateCreditCardTransaction(String id, CreditCardTransactionRequest creditCardTransactionRequest) {
        log.info("Updating credit card transaction for id: {}", id);
        return creditCardTransactionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card transaction with id: " + id + " not found")))
                .flatMap(existingTransaction ->
                        creditCardTransactionRepository.save(
                                updateCreditCardTransactionFromRequest(existingTransaction, creditCardTransactionRequest))
                ).map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    @Override
    public Mono<Void> deleteCreditCardTransactionById(String id) {
        log.info("Deleting credit card transaction by id: {}", id);
        return creditCardTransactionRepository.deleteById(id);
    }

    @Override
    public Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber) {
        log.info("Fetching credit card transactions for credit card number: {}", creditCardNumber);
        return creditCardTransactionRepository.findByCreditCardNumber(creditCardNumber)
                .sort(Comparator.comparing(CreditCardTransaction::getCreatedAt).reversed())
                .map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    @Override
    public Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumberLast10(String creditCardNumber) {
        log.info("Fetching last 10 credit card transactions for credit card number: {}", creditCardNumber);
        return creditCardTransactionRepository.findByCreditCardNumber(creditCardNumber)
                .sort(Comparator.comparing(CreditCardTransaction::getCreatedAt).reversed())
                .take(10)
                .map(creditCardTransactionMapper::mapToCreditCardTransactionResponse);
    }

    public CreditCardTransaction updateCreditCardTransactionFromRequest(CreditCardTransaction existingTransaction,
                                                                        CreditCardTransactionRequest creditCardTransactionRequest) {
        CreditCardTransaction creditCardTransaction = creditCardTransactionMapper.mapToCreditCardTransaction(creditCardTransactionRequest);
        creditCardTransaction.setId(existingTransaction.getId());
        creditCardTransaction.setCreatedAt(existingTransaction.getCreatedAt());
        return creditCardTransaction;
    }
}
