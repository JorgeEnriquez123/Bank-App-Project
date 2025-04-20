package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditMapper;
import com.jorge.credits.mapper.TransactionRequestMapper;
import com.jorge.credits.model.*;
import com.jorge.credits.repository.CreditRepository;
import com.jorge.credits.service.CreditService;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.dto.request.AccountBalanceUpdateRequest;
import com.jorge.credits.webclient.dto.response.AccountResponse;
import com.jorge.credits.webclient.dto.response.CustomerResponse;
import com.jorge.credits.webclient.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@RequiredArgsConstructor
@Slf4j
@Service
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
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with Id: " + creditRequest.getCreditHolderId() + " not found")))
                .flatMap(customerResponse -> {
                    if (customerResponse.getCustomerType() == CustomerResponse.CustomerType.PERSONAL) {
                        log.info("Customer is of type PERSONAL. Only one credit");
                        return creditRepository.findByCreditHolderIdAndCreditTypeIn(
                                        customerResponse.getId(),
                                        List.of(Credit.CreditType.PERSONAL, Credit.CreditType.BUSINESS))
                                .hasElements()
                                .flatMap(hasCredit -> {
                                    if (hasCredit) {
                                        log.warn("Customer with dni: {} already has a credit", customerResponse.getDni());
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Customer with dni: " + customerResponse.getDni() + " has one credit already"));
                                    } else {
                                        return creditRepository.save(credit); // If there are no credits, save a new one
                                    }
                                });
                    } else {
                        log.info("Customer is of type BUSINESS. Unlimited Credits");
                        return creditRepository.findByCreditHolderId(customerResponse.getId())
                                .any(creditAvailable -> creditAvailable.getDueDate().isBefore(LocalDate.now()))
                                .flatMap(hasOverdueDebt -> {
                                    if (hasOverdueDebt) {
                                        log.warn("Customer with dni: {} has overdue debts", customerResponse.getDni());
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Customer with dni: " + customerResponse.getDni() + " has overdue debts"));
                                    } else {
                                        return creditRepository.save(credit);
                                    }
                                });
                    }
                })
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
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> payCreditById(String id, CreditPaymentRequest creditPaymentRequest) {
        log.info("Paying credit with id: {} using account number: {}, amount: {}",
                id, creditPaymentRequest.getAccountNumber(), creditPaymentRequest.getAmount());
        // Quick check for credit type
        if(creditPaymentRequest.getCreditType() != CreditPaymentRequest.CreditTypeEnum.CREDIT_PAYMENT)
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credit type. Only CREDIT_PAYMENT is allowed"));

        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit with id " + id + " not found")))
                .flatMap(credit -> validateAndUpdateCredit(credit, creditPaymentRequest.getAmount()).flatMap(
                        updateCredit -> {
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
                )
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> payCreditByIdWithDebitCard(String id, CreditPaymentByDebitCardRequest creditPaymentRequest) {
        log.info("Paying credit with id: {} using debit card number: {}, amount: {}", id,
                creditPaymentRequest.getDebitCardNumber(), creditPaymentRequest.getAmount());
        if (creditPaymentRequest.getCreditType() != CreditPaymentByDebitCardRequest.CreditTypeEnum.CREDIT_PAYMENT)
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credit type. Only CREDIT_PAYMENT is allowed"));
        return creditRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit with id " + id + " not found")))
                .flatMap(credit -> validateAndUpdateCredit(credit, creditPaymentRequest.getAmount())
                        .flatMap(validatedCredit -> accountClient.getDebitCardByDebitCardNumber(creditPaymentRequest.getDebitCardNumber())
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Debit Card with number: " + creditPaymentRequest.getDebitCardNumber() + " not found")))
                                .flatMap(debitCard ->
                                        accountClient.getAccountByAccountNumber(debitCard.getMainLinkedAccountNumber())
                                        .flatMap(mainAccount -> {
                                            log.info("Checking if main account has sufficient balance");
                                            if (mainAccount.getBalance().compareTo(creditPaymentRequest.getAmount()) > 0) {
                                                log.info("Starting payment with main account");
                                                return startCreditPaymentWithDebitCard(validatedCredit, mainAccount, creditPaymentRequest);
                                            } else {
                                                log.info("Checking if other linked accounts have sufficient balance");
                                                return Flux.fromIterable(debitCard.getLinkedAccountsNumber())
                                                        .filter(accountNumber ->
                                                                !accountNumber.equals(debitCard.getMainLinkedAccountNumber()))
                                                        .flatMap(accountClient::getAccountByAccountNumber)
                                                        .filter(account ->
                                                                account.getBalance().compareTo(creditPaymentRequest.getAmount()) >= 0)
                                                        .next()
                                                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                "Debit Card does not have enough balance in any of its linked accounts")))
                                                        .flatMap(accountWithSufficientBalance -> {
                                                            log.info("Starting payment with linked account: {}",
                                                                    accountWithSufficientBalance.getAccountNumber());
                                                            return startCreditPaymentWithDebitCard(validatedCredit,
                                                                        accountWithSufficientBalance, creditPaymentRequest);
                                                        });
                                            }
                                        })
                                )
                        )
                )
                .map(creditMapper::mapToCreditResponse);
    }

    private Mono<Credit> startCreditPaymentWithDebitCard(Credit credit, AccountResponse mainAccount, CreditPaymentByDebitCardRequest creditPaymentRequest) {
        AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                AccountBalanceUpdateRequest.builder().balance(creditPaymentRequest.getAmount()).build();
        return accountClient.reduceBalanceByAccountNumber(
                        mainAccount.getAccountNumber(), accountBalanceUpdateRequest)
                .flatMap(accountBalanceResponse -> creditRepository.save(credit))
                .flatMap(savedCredit -> {
                    TransactionRequest transactionRequest = transactionRequestMapper.mapDebitCardPaymentRequestToTransactionRequest(
                            mainAccount.getAccountNumber(),
                            creditPaymentRequest,
                            savedCredit.getId(),
                            "Credit payment for credit id: " + savedCredit.getId(),
                            BigDecimal.ZERO);
                    return transactionClient.createTransaction(transactionRequest)
                            .thenReturn(savedCredit);
                });
    }

    private Mono<Credit> validateAndUpdateCredit(Credit credit, BigDecimal paymentAmount) {
        if (credit.getStatus() == Credit.Status.PAID) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit is already PAID"));
        }
        // Subtract the payment amount from the credit amount
        BigDecimal remainingAmount = credit.getCreditAmount().subtract(paymentAmount);

        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds credit amount"));
        } else if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            credit.setStatus(Credit.Status.PAID);
        }

        credit.setCreditAmount(remainingAmount);
        return Mono.just(credit);
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
