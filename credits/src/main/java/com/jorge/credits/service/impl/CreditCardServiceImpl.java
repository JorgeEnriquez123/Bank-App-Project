package com.jorge.credits.service.impl;

import com.jorge.credits.mapper.CreditCardMapper;
import com.jorge.credits.mapper.TransactionRequestMapper;
import com.jorge.credits.model.*;
import com.jorge.credits.repository.CreditCardRepository;
import com.jorge.credits.service.CreditCardService;
import com.jorge.credits.webclient.client.AccountClient;
import com.jorge.credits.webclient.client.CustomerClient;
import com.jorge.credits.webclient.client.TransactionClient;
import com.jorge.credits.webclient.model.*;
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
public class CreditCardServiceImpl implements CreditCardService {
    private final AccountClient accountClient;
    private final CustomerClient customerClient;
    private final TransactionClient transactionClient;

    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;
    private final TransactionRequestMapper transactionRequestMapper;


    @Override
    public Flux<CreditCardResponse> getAllCreditCards() {
        log.info("Fetching all credits");
        return creditCardRepository.findAll()
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<CreditCardResponse> createCreditCard(CreditCardRequest creditCardRequest) {
        log.info("Creating a new credit card for customer DNI: {}", creditCardRequest.getCardHolderId());
        CreditCard credit = creditCardMapper.mapToCreditCard(creditCardRequest);

        return customerClient.getCustomerById(creditCardRequest.getCardHolderId())
                .flatMap(customerResponse -> {
                    if (customerResponse.getCustomerType() == CustomerResponse.CustomerType.PERSONAL) {
                        log.info("Customer is of type PERSONAL. Only PERSONAL CREDIT CARDs are allowed.");
                        if(creditCardRequest.getType() == CreditCardRequest.TypeEnum.BUSINESS_CREDIT_CARD) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Customer with dni: " + customerResponse.getDni() + " is PERSONAL. They can't have a BUSINESS CREDIT CARD"));
                        }
                        else{
                            return creditCardRepository.save(credit);
                        }
                    }
                    else if (customerResponse.getCustomerType() == CustomerResponse.CustomerType.BUSINESS){
                        log.info("Customer is of type BUSINESS. PERSONAL and BUSINESS Cards are allowed.");
                        return creditCardRepository.save(credit);
                    }
                    else {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Invalid customer type: " + customerResponse.getCustomerType()));
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer with Id: " + creditCardRequest.getCardHolderId() + " not found")))
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<CreditCardResponse> getCreditCardById(String id) {
        log.info("Fetching credit card by Id: {}", id);
        return creditCardRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card with id: " + id + " not found")))
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<CreditCardResponse> updateCreditCardById(String id, CreditCardRequest creditCardRequest) {
        log.info("Updating credit card with id: {}", id);
        return creditCardRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card with id: " + id + " not found")))
                .flatMap(existingCreditCard -> creditCardRepository.save(
                        updateCreditCardFromRequest(existingCreditCard, creditCardRequest)))
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<Void> deleteCreditCardById(String id) {
        log.info("Deleting credit Card with id: {}", id);
        return creditCardRepository.deleteById(id);
    }

    @Override
    public Flux<CreditCardResponse> getCreditCardsByCardHolderId(String cardHolderId) {
        log.info("Fetching credit cards by customer Id: {}", cardHolderId);
        return creditCardRepository.findByCardHolderId(cardHolderId)
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<BalanceResponse> getCreditCardAvailableBalanceByCreditCardNumber(String creditCardNumber) {
        log.info("Fetching available balance by credit card number: {}", creditCardNumber);
        return creditCardRepository.findByCreditCardNumber(creditCardNumber)
                .flatMap(creditCard -> {
                    BalanceResponse balanceResponse = new BalanceResponse();
                    balanceResponse.setCreditCardNumber(String.valueOf(creditCard.getCreditCardNumber()));
                    balanceResponse.setAvailableBalance(creditCard.getAvailableBalance());
                    balanceResponse.setOutstandingBalance(creditCard.getOutstandingBalance());
                    return Mono.just(balanceResponse);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Credit card with number : " + creditCardNumber + " not found")));
    }

    @Override
    public Mono<CreditCardResponse> payCreditCardByCreditCardNumber(String creditCardNumber, CreditPaymentRequest creditPaymentRequest) {
        log.info("Paying credit card with number: {} using account number: {}, amount: {}", creditCardNumber, creditPaymentRequest.getAccountNumber(), creditPaymentRequest.getAmount());
        if(creditPaymentRequest.getCreditType() != CreditPaymentRequest.CreditTypeEnum.CREDIT_CARD_PAYMENT)
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credit type. Only CREDIT_CARD_PAYMENT is allowed"));
        return creditCardRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card with number " + creditCardNumber + " not found")))
                .flatMap(creditCard -> {
                    BigDecimal newOutstandingBalance = creditCard.getOutstandingBalance().subtract(creditPaymentRequest.getAmount());

                    if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds outstanding balance"));
                    }

                    creditCard.setOutstandingBalance(newOutstandingBalance);
                    creditCard.setAvailableBalance(creditCard.getAvailableBalance().add(creditPaymentRequest.getAmount()));

                    AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                            AccountBalanceUpdateRequest.builder().balance(creditPaymentRequest.getAmount()).build();

                    return accountClient.reduceBalanceByAccountNumber(creditPaymentRequest.getAccountNumber(), accountBalanceUpdateRequest)
                            .flatMap(accountBalanceResponse -> creditCardRepository.save(creditCard))
                            .flatMap(savedCreditCard -> {
                                log.info("Creating Payment Transaction made by Account number: {}", creditPaymentRequest.getAccountNumber());
                                TransactionRequest transactionRequest = transactionRequestMapper.mapPaymentRequestToTransactionRequest(
                                        creditPaymentRequest,
                                        savedCreditCard.getId(),
                                        "Credit card payment for credit card: " + creditCard.getCreditCardNumber(),
                                        BigDecimal.ZERO);
                                return transactionClient.createTransaction(transactionRequest)
                                        .thenReturn(savedCreditCard);
                            })
                            .flatMap(savedCreditCard -> {
                                log.info("Creating Credit Card Transaction for Credit Card with number: {}", creditCardNumber);
                                CreditCardTransactionRequest creditCardTransactionRequest =
                                        transactionRequestMapper.mapPaymentRequestToCreditCardTransactionRequest(
                                                creditPaymentRequest, creditCardNumber);
                                return transactionClient.createCreditCardTransaction(creditCardTransactionRequest)
                                        .thenReturn(savedCreditCard);
                            });
                })
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Mono<CreditCardResponse> payCreditCardWithDebitCard(String creditCardNumber, CreditPaymentByDebitCardRequest creditCardPaymentRequest) {
        log.info("Paying credit card with number: {} using debit card number: {}, amount: {}", creditCardNumber, creditCardPaymentRequest.getDebitCardNumber(), creditCardPaymentRequest.getAmount());
        if(creditCardPaymentRequest.getCreditType() != CreditPaymentByDebitCardRequest.CreditTypeEnum.CREDIT_CARD_PAYMENT)
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credit type. Only CREDIT_CARD_PAYMENT is allowed"));
        return creditCardRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card with number " + creditCardNumber + " not found")))
                .flatMap(creditCard -> validateAndUpdateCreditCard(creditCard, creditCardPaymentRequest.getAmount())
                        .flatMap(validatedCreditCard -> accountClient.getDebitCardByDebitCardNumber(creditCardPaymentRequest.getDebitCardNumber())
                                .flatMap(debitCard -> accountClient.getAccountByAccountNumber(debitCard.getMainLinkedAccountNumber())
                                        .flatMap(mainAccount -> {
                                            if (mainAccount.getBalance().compareTo(creditCardPaymentRequest.getAmount()) >= 0) {
                                                return startCreditCardPaymentWithDebitCard(validatedCreditCard, mainAccount, creditCardPaymentRequest);
                                            } else {
                                                return Flux.fromIterable(debitCard.getLinkedAccountsNumber())
                                                        .filter(accountNumber -> !accountNumber.equals(debitCard.getMainLinkedAccountNumber()))
                                                        .flatMap(accountClient::getAccountByAccountNumber)
                                                        .filter(account -> account.getBalance().compareTo(creditCardPaymentRequest.getAmount()) >= 0)
                                                        .next()
                                                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit Card does not have enough balance in any of its linked accounts")))
                                                        .flatMap(accountWithSufficientBalance -> startCreditCardPaymentWithDebitCard(validatedCreditCard, accountWithSufficientBalance, creditCardPaymentRequest));
                                            }
                                        })
                                )
                        )
                )
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    private Mono<CreditCard> startCreditCardPaymentWithDebitCard(CreditCard creditCard, AccountResponse account, CreditPaymentByDebitCardRequest creditPaymentByDebitCardRequest) {

        BigDecimal paymentAmount = creditPaymentByDebitCardRequest.getAmount();
        AccountBalanceUpdateRequest accountBalanceUpdateRequest =
                AccountBalanceUpdateRequest.builder().balance(paymentAmount).build();

        return accountClient.reduceBalanceByAccountNumber(account.getAccountNumber(), accountBalanceUpdateRequest)
                .flatMap(accountBalanceResponse -> {
                    //Update credit Card Balance
                    BigDecimal newOutstandingBalance = creditCard.getOutstandingBalance().subtract(paymentAmount);
                    creditCard.setOutstandingBalance(newOutstandingBalance);
                    creditCard.setAvailableBalance(creditCard.getAvailableBalance().add(paymentAmount));
                    return creditCardRepository.save(creditCard);
                })
                .flatMap(savedCreditCard -> {
                    log.info("Creating Payment Transaction made by Account number: {}", account.getAccountNumber());
                    TransactionRequest transactionRequest = transactionRequestMapper.mapDebitCardPaymentRequestToTransactionRequest(
                            account.getAccountNumber(),
                            creditPaymentByDebitCardRequest,
                            savedCreditCard.getId(),
                            "Credit card payment for credit card: " + savedCreditCard.getCreditCardNumber() + " using debit card.",
                            BigDecimal.ZERO);
                    return transactionClient.createTransaction(transactionRequest)
                            .thenReturn(savedCreditCard);
                })
                .flatMap(savedCreditCard -> {
                    log.info("Creating Credit Card Transaction for Credit Card with number: {}", savedCreditCard.getCreditCardNumber());
                    CreditCardTransactionRequest creditCardTransactionRequest =
                            transactionRequestMapper.mapPaymentDebitCardRequestToCreditCardTransactionRequest(
                                    creditPaymentByDebitCardRequest, savedCreditCard.getCreditCardNumber());
                    return transactionClient.createCreditCardTransaction(creditCardTransactionRequest)
                            .thenReturn(savedCreditCard);
                });
    }

    private Mono<CreditCard> validateAndUpdateCreditCard(CreditCard creditCard, BigDecimal paymentAmount) {
        BigDecimal newOutstandingBalance = creditCard.getOutstandingBalance().subtract(paymentAmount);
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exceeds outstanding balance"));
        }
        return Mono.just(creditCard);
    }

    @Override
    public Mono<CreditCardResponse> consumeCreditCardByCreditCardNumber(String creditCardNumber, ConsumptionRequest consumptionRequest) {
        log.info("Consuming credit card with number: {}, amount: {}", creditCardNumber, consumptionRequest.getAmount());
        BigDecimal consumptionAmount = consumptionRequest.getAmount();
        return creditCardRepository.findByCreditCardNumber(creditCardNumber)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card with number " + creditCardNumber + " not found")))
                .flatMap(creditCard -> {
                    if (creditCard.getStatus() != CreditCard.CreditCardStatus.ACTIVE) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit card is not active"));
                    }

                    if (creditCard.getAvailableBalance().compareTo(consumptionAmount) < 0) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available balance"));
                    }

                    creditCard.setAvailableBalance(creditCard.getAvailableBalance().subtract(consumptionAmount));
                    creditCard.setOutstandingBalance(creditCard.getOutstandingBalance().add(consumptionAmount));

                    return creditCardRepository.save(creditCard)
                            .flatMap(savedCreditCard -> {
                                CreditCardTransactionRequest creditCardTransactionRequest = transactionRequestMapper.
                                        mapConsumeRequestToTransactionRequest(
                                        consumptionRequest,
                                        creditCardNumber
                                );
                                return transactionClient.createCreditCardTransaction(creditCardTransactionRequest)
                                        .thenReturn(savedCreditCard);
                            });
                })
                .map(creditCardMapper::mapToCreditCardResponse);
    }

    @Override
    public Flux<CreditCardTransactionResponse> getCreditCardTransactionsByCreditCardNumber(String creditCardNumber) {
        return transactionClient.getCreditCardTransactionsByCreditCardNumber(creditCardNumber);
    }

    private CreditCard updateCreditCardFromRequest(CreditCard existingCreditCard, CreditCardRequest creditCardRequest) {
        CreditCard updatedCreditCard = creditCardMapper.mapToCreditCard(creditCardRequest);
        updatedCreditCard.setId(existingCreditCard.getId());
        updatedCreditCard.setCreatedAt(existingCreditCard.getCreatedAt());
        return updatedCreditCard;
    }
}
