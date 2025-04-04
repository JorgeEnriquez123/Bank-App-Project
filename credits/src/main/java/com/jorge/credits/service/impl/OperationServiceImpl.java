package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditMapper;
import com.jorge.credits.model.Credit;
import com.jorge.credits.model.CreditPaymentRequest;
import com.jorge.credits.model.CreditResponse;
import com.jorge.credits.model.TransactionResponse;
import com.jorge.credits.repository.CreditRepository;
import com.jorge.credits.service.OperationService;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.model.AccountBalanceUpdateRequest;
import com.jorge.credits.webclient.model.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationServiceImpl implements OperationService {
    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    private final CreditMapper creditMapper;
    private final CreditRepository creditRepository;

    @Override
    public Flux<TransactionResponse> getTransactionsByCreditId(String creditId) {
        log.info("Fetching transactions by credit id: {}", creditId);
        return transactionClient.getTransactionsByCreditId(creditId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No transactions found with credit id: " + creditId)));
    }

    @Override
    public Mono<CreditResponse> payCreditById(String creditId, CreditPaymentRequest creditPaymentRequest) {
        log.info("Paying credit with id: {} using account number: {}, amount: {}", creditId, creditPaymentRequest.getAccountNumber(), creditPaymentRequest.getAmount());
        return creditRepository.findById(creditId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit with id " + creditId + " not found")))
                .flatMap(credit -> {
                    if (credit.getStatus() != Credit.Status.ACTIVE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit is not active"));
                    }
                    //If the credit is a credit card, it won't proceed
                    if (credit.getCreditType() == Credit.CreditType.PERSONAL_CREDIT_CARD ||
                            credit.getCreditType() == Credit.CreditType.BUSINESS_CREDIT_CARD) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit cards are paid by their card number"));
                    }

                    BigDecimal remainingAmount = credit.getCreditAmount().subtract(creditPaymentRequest.getAmount());

                    if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds credit amount"));
                    }
                    if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
                        credit.setStatus(Credit.Status.PAID);
                    }

                    credit.setCreditAmount(remainingAmount);

                    AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                            AccountBalanceUpdateRequest.builder().balance(creditPaymentRequest.getAmount()).build();

                    return accountClient.reduceBalanceByAccountNumber(creditPaymentRequest.getAccountNumber(), accountBalanceUpdateRequest)
                            .flatMap(accountBalanceResponse -> creditRepository.save(credit))
                            .flatMap(savedCredit -> {
                                TransactionRequest transactionRequest = createTransactionRequest(
                                        creditPaymentRequest.getAccountNumber(),
                                        savedCredit.getId(),
                                        creditPaymentRequest.getAmount(),
                                        TransactionRequest.TransactionType.CREDIT_PAYMENT,
                                        "Credit payment for id: " + savedCredit.getId()
                                );
                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedCredit);
                            });
                })
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> payCreditCardByCreditCardNumber(String creditCardNumber, CreditPaymentRequest creditPaymentRequest) {
        log.info("Paying credit card with number: {} using account number: {}, amount: {}", creditCardNumber, creditPaymentRequest.getAccountNumber(), creditPaymentRequest.getAmount());
        return creditRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card with number " + creditCardNumber + " not found")))
                .flatMap(credit -> {
                    if (credit.getStatus() != Credit.Status.ACTIVE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit card is not active"));
                    }

                    if (credit.getCreditType() != Credit.CreditType.PERSONAL_CREDIT_CARD && credit.getCreditType() != Credit.CreditType.BUSINESS_CREDIT_CARD) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit is not a credit card"));
                    }

                    BigDecimal newOutstandingBalance = credit.getOutstandingBalance().subtract(creditPaymentRequest.getAmount());

                    if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds outstanding balance"));
                    }

                    credit.setOutstandingBalance(newOutstandingBalance);
                    credit.setAvailableBalance(credit.getAvailableBalance().add(creditPaymentRequest.getAmount()));

                    AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                            AccountBalanceUpdateRequest.builder().balance(creditPaymentRequest.getAmount()).build();

                    return accountClient.reduceBalanceByAccountNumber(creditPaymentRequest.getAccountNumber(), accountBalanceUpdateRequest)
                            .flatMap(accountBalanceResponse -> creditRepository.save(credit))
                            .flatMap(savedCredit -> {
                                TransactionRequest transactionRequest = createTransactionRequest(
                                        creditPaymentRequest.getAccountNumber(),
                                        savedCredit.getId(),
                                        creditPaymentRequest.getAmount(),
                                        TransactionRequest.TransactionType.CREDIT_CARD_PAYMENT,
                                        "Credit card payment for credit card: " + credit.getCreditCardNumber()
                                );
                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedCredit);
                            });
                })
                .map(creditMapper::mapToCreditResponse);
    }

    @Override
    public Mono<CreditResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, BigDecimal amount) {
        log.info("Consuming credit card with number: {}, amount: {}", creditCardNumber, amount);
        return creditRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card with number " + creditCardNumber + " not found")))
                .flatMap(credit -> {
                    if (credit.getStatus() != Credit.Status.ACTIVE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit card is not active"));
                    }

                    if (credit.getCreditType() != Credit.CreditType.PERSONAL_CREDIT_CARD && credit.getCreditType() != Credit.CreditType.BUSINESS_CREDIT_CARD) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit is not a credit card"));
                    }

                    if (credit.getAvailableBalance().compareTo(amount) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available balance"));
                    }

                    credit.setAvailableBalance(credit.getAvailableBalance().subtract(amount));
                    credit.setOutstandingBalance(credit.getOutstandingBalance().add(amount));

                    return creditRepository.save(credit)
                            .flatMap(savedCredit -> {
                                TransactionRequest transactionRequest = createCreditCartTransactionRequest(
                                        savedCredit.getId(),
                                        amount,
                                        TransactionRequest.TransactionType.CREDIT_CARD_CONSUMPTION,
                                        "Credit card consumption for credit card: " + credit.getCreditCardNumber()
                                );
                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedCredit);
                            });
                })
                .map(creditMapper::mapToCreditResponse);
    }

    private TransactionRequest createTransactionRequest(String accountNumber,
                                                        String creditId, BigDecimal amount,
                                                        TransactionRequest.TransactionType transactionType,
                                                        String description) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(accountNumber);
        return getTransactionRequest(creditId, amount, transactionType, description, transactionRequest);
    }

    private TransactionRequest getTransactionRequest(String creditId, BigDecimal amount, TransactionRequest.TransactionType transactionType, String description, TransactionRequest transactionRequest) {
        transactionRequest.setCreditId(creditId);
        transactionRequest.setAmount(amount);
        transactionRequest.setCurrencyType(TransactionRequest.CurrencyType.PEN);
        transactionRequest.setTransactionType(transactionType);
        transactionRequest.setStatus(TransactionRequest.TransactionStatus.COMPLETED);
        transactionRequest.setDescription(description);
        transactionRequest.setFee(BigDecimal.ZERO);
        return transactionRequest;
    }

    private TransactionRequest createCreditCartTransactionRequest(String creditId, BigDecimal amount,
                                                        TransactionRequest.TransactionType transactionType,
                                                        String description) {
        TransactionRequest transactionRequest = new TransactionRequest();
        return getTransactionRequest(creditId, amount, transactionType, description, transactionRequest);
    }
}
