package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditMapper;
import com.jorge.credits.mapper.TransactionRequestMapper;
import com.jorge.credits.model.*;
import com.jorge.credits.repository.CreditRepository;
import com.jorge.credits.service.CreditService;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.model.AccountBalanceUpdateRequest;
import com.jorge.credits.webclient.model.CustomerResponse;
import com.jorge.credits.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl implements CreditService {
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final TransactionClient transactionClient;
    private final CreditMapper creditMapper;
    private final CreditRepository creditRepository;
    private final TransactionRequestMapper transactionRequestMapper;

    @Override
    public Flux<CreditResponse> getAllCredits() {
        log.info("Fetching all credits");
        return creditRepository.findAll()
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> createCredit(CreditRequest creditRequest) {
        log.info("Creating a new credit for customer DNI: {}", creditRequest.getCreditHolderId());
        Credit credit = creditMapper.mapToCredit(creditRequest);

        return customerClient.getCustomerById(creditRequest.getCreditHolderId())
                .flatMap(customerResponse -> {
                    if (customerResponse.getCustomerType() == CustomerResponse.CustomerType.PERSONAL) {
                        log.info("Customer is of type PERSONAL. Only one credit");
                        return creditRepository.findByCreditHolderIdAndCreditTypeIn(
                                        customerResponse.getId(),
                                        List.of(Credit.CreditType.PERSONAL, Credit.CreditType.BUSINESS))
                                .hasElements()
                                .flatMap(hasCredit -> {
                                    if (hasCredit) {
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Customer with dni: " + customerResponse.getDni() + " has one credit already"));
                                    } else {
                                        return creditRepository.save(credit); // If there are no credits, save a new one
                                    }
                                });
                    } else {
                        log.info("Customer is of type BUSINESS. Unlimited Credits");
                        return creditRepository.save(credit);
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with Id: " + creditRequest.getCreditHolderId() + " not found")))
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> getCreditById(String id) {
        log.info("Fetching credit by Id: {}", id);
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit with id: " + id + " not found")))
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> updateCreditById(String id, CreditRequest creditRequest) {
        log.info("Updating credit with id: {}", id);
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit with id: " + id + " not found")))
                .flatMap(existingCredit -> creditRepository.save(
                        updateCreditFromRequest(existingCredit, creditRequest)))
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<Void> deleteCreditById(String id) {
        log.info("Deleting credit with id: {}", id);
        return creditRepository.deleteById(id);
    }

    @Override
    public Flux<CreditResponse> getCreditsByCreditHolderId(String creditHolderId) {
        log.info("Fetching credits by customer Id: {}", creditHolderId);
        return creditRepository.findByCreditHolderId(creditHolderId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credits from customer by Id: " + creditHolderId + " not found")))
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> payCreditById(String id, CreditPaymentRequest creditPaymentRequest) {
        log.info("Paying credit with id: {} using account number: {}, amount: {}", id, creditPaymentRequest.getAccountNumber(), creditPaymentRequest.getAmount());
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit with id " + id + " not found")))
                .flatMap(credit -> {
                    if (credit.getStatus() == Credit.Status.PAID) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit is already PAID"));
                    }
                    BigDecimal remainingAmount = credit.getCreditAmount().subtract(creditPaymentRequest.getAmount());

                    if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds credit amount"));
                    } else if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
                        credit.setStatus(Credit.Status.PAID);
                    }

                    credit.setCreditAmount(remainingAmount);

                    AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                            AccountBalanceUpdateRequest.builder().balance(creditPaymentRequest.getAmount()).build();

                    return accountClient.reduceBalanceByAccountNumber(
                            creditPaymentRequest.getAccountNumber(), accountBalanceUpdateRequest)
                            .flatMap(accountBalanceResponse -> creditRepository.save(credit))
                            .flatMap(savedCredit -> {
                                TransactionRequest transactionRequest = transactionRequestMapper.mapPaymentRequestToTransactionRequest(
                                        creditPaymentRequest,
                                        savedCredit.getId(),
                                        "Credit payment for credit id: " + savedCredit.getId(),
                                        BigDecimal.ZERO);
                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedCredit);
                            });
                })
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String id) {
        log.info("Fetching transactions by credit id: {}", id);
        return transactionClient.getTransactionsByCreditId(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No transactions found with credit id: " + id)));
    }

    private Credit updateCreditFromRequest(Credit existingCredit, CreditRequest creditRequest) {
        Credit updatedCredit = creditMapper.mapToCredit(creditRequest);
        updatedCredit.setId(existingCredit.getId());
        updatedCredit.setCreatedAt(existingCredit.getCreatedAt());
        return updatedCredit;
    }
}
